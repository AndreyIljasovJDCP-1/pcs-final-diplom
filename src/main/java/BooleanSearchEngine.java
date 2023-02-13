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
     *
     * @param pdfsDir директория индексируемых файлов
     * @throws IOException      if I/O error occurs
     */
    public BooleanSearchEngine(File pdfsDir) throws IOException {
        try (var stream = Files.newDirectoryStream(pdfsDir.toPath())) {
            var listStopWords = Files.readAllLines(Path.of("stop-ru.txt"));
            wordsMap = new HashMap<>();
            stopList = new HashSet<>(listStopWords);

            for (Path entry : stream) {
                var file = entry.toFile();
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
        if (wordsMap.isEmpty()) throw new IOException(pdfsDir.getAbsolutePath()
                + " - директория индексируемых файлов пуста.");
    }

    /**
     * Поиск по запросу без учета регистра.
     * Аккумуляция результатов через Collectors.toMap
     * (элементы группируются по ключу: (имя файла:страница),
     * у дубликатов суммируется значение поля count и возвращается 1-й элемент)
     *
     * @param request запрос (одно или несколько слов для поиска)
     * @return список ответов pageEntry,
     * отсортированный по кол-ву вхождений в убывающем порядке
     */
    @Override
    public List<PageEntry> search(String request) {

        var wordsList = Arrays.stream(request
                        .toLowerCase()
                        .split("\\P{IsAlphabetic}+"))
                .filter(word -> !stopList.contains(word))
                .distinct()
                .collect(Collectors.toList());

        var pageEntryList = wordsList.stream()
                .map(wordsMap::get)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        var pageEntryMap = pageEntryList.stream()
                .collect(Collectors.toMap(
                        pe -> pe.getPdfName() + ":" + pe.getPage(), // ключ (файл:стр)
                        pe -> pe, // объект pageEntry
                        (pe1, pe2) -> { //суммируем count при совпадении ключей
                            pe1.setCount(pe1.getCount() + pe2.getCount());
                            return pe1; //возвращаем скорректированный pageEntry
                        },
                        HashMap::new));

        return pageEntryMap.values().stream().sorted().collect(Collectors.toList());
    }
}