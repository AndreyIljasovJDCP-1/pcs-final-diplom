import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Класс, описывающий поисковый движок.
 * <p>Реализует булевый регистронезависимый поиск по запросу.
 * В основе алгоритма лежит заполнение карты
 * возможных результатов поиска при создании движка (индексация).</p>
 * <p>Поле <b>wordsMap</b> - карта возможных результатов поиска.</p>
 * <p>Поле <b>stopList</b> - множество стоп-слов.</p>
 */
public class BooleanSearchEngine implements SearchEngine {
    private final Map<String, List<PageEntry>> wordsMap;
    private final Set<String> stopList;
    /**
     * Конструктор - создание поискового движка(индексация).
     * <p> Сохранение возможных результатов поиска в <b>wordsMap</b>.</p>
     * <p> Формирование множества стоп-слов <b>stopList</b>.</p>
     * @param pdfsDir директория индексируемых файлов
     * @throws IOException      if I/O error occurs
     * @throws RuntimeException if denoted directory's empty or not exists
     */
    public BooleanSearchEngine(File pdfsDir) throws IOException {
        var fileList = pdfsDir.listFiles();
        if (fileList == null || fileList.length == 0) {
            throw new RuntimeException(pdfsDir
                    + ": директория индексируемых файлов пуста или не существует.");
        }
        var listStopWords = Files.readAllLines(Path.of("stop-ru.txt"));
        wordsMap = new HashMap<>();
        stopList = new HashSet<>(listStopWords);

        for (File file : fileList) {
            var doc = new PdfDocument(new PdfReader(file));
            var pagesTotal = doc.getNumberOfPages();
            var fileName = file.getName();

            for (int pageNum = 1; pageNum <= pagesTotal; pageNum++) {
                var pdfPage = doc.getPage(pageNum);
                var text = PdfTextExtractor.getTextFromPage(pdfPage);
                var words = text.toLowerCase().split("\\P{IsAlphabetic}+");

                Map<String, Integer> frequencyMap = new HashMap<>();

                for (String word : words) {
                    if (word.isEmpty()
                            || stopList.contains(word)) {
                        continue;
                    }
                    frequencyMap.merge(word, 1, Integer::sum);
                }
                for (Map.Entry<String, Integer> kv : frequencyMap.entrySet()) {
                    var pageEntry = new PageEntry(fileName, pageNum, kv.getValue());
                    if (!wordsMap.containsKey(kv.getKey())) {
                        wordsMap.put(kv.getKey(), new ArrayList<>(List.of(pageEntry)));
                    } else {
                        wordsMap.get(kv.getKey()).add(pageEntry);
                    }
                }
            }
        }
    }

    /**
     * Поиск по запросу без учета регистра.
     * Аккумуляция результатов через сортировку,
     * итерацию по списку и суммирование поля count одинаковых страниц,
     * с последующей фильтрацией дублирующих страниц.
     *
     * @param words запрос (одно или несколько слов для поиска)
     * @return список ответов pageEntry,
     * отсортированный по кол-ву вхождений в убывающем порядке
     */
    @Override
    public List<PageEntry> search(String words) {

        var wordsList = Arrays.stream(words
                        .toLowerCase()
                        .split("\\P{IsAlphabetic}+"))
                .filter(word -> !stopList.contains(word))
                .distinct()
                .collect(Collectors.toList());

        var pageEntryList = wordsList.stream()
                .map(wordsMap::get)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .sorted(Comparator
                        .comparing(PageEntry::getPdfName)
                        .thenComparing(PageEntry::getPage))
                .collect(Collectors.toList());

        for (int i = 0; i < pageEntryList.size() - 1; i++) {
            var current = pageEntryList.get(i);
            var next = pageEntryList.get(i + 1);
            if (current.getPdfName().equals(next.getPdfName())
                    && current.getPage() == next.getPage()) {
                next.setCount(next.getCount() + current.getCount());
                current.setCount(0);
            }
        }

        return pageEntryList.stream()
                .filter(pageEntry -> pageEntry.getCount() > 0)
                .sorted()
                .collect(Collectors.toList());
    }
}