import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Класс, описывающий поисковый движок.
 * <p>Реализует булевый регистронезависимый поиск по слову.
 * В основе алгоритма лежит заполнение карты
 * возможных результатов поиска при создании движка (индексация).</p>
 * <p>Поля: <b>wordsMap</b> - карта возможных результатов поиска
 */
public class BooleanSearchEngine implements SearchEngine {
    Map<String, List<PageEntry>> wordsMap;

    /**
     * Конструктор - создание поискового движка(индексация).
     * <p>Сохранение возможных результатов поиска в <b>wordsMap</b>.</p>
     *
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

        wordsMap = new HashMap<>();

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
                    if (word.isEmpty()) {
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
     * Поиск по слову без учета регистра.
     *
     * @param word слово для поиска
     * @return список ответов pageEntry,
     * отсортированный по кол-ву вхождений в убывающем порядке
     */
    @Override
    public List<PageEntry> search(String word) {
        var result = wordsMap.get(word.toLowerCase());
        if (result != null) {
            Collections.sort(result);
        } else {
            result = new ArrayList<>();
        }
        return result;
    }
}