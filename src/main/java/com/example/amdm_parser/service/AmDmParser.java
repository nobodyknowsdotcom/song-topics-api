package com.example.amdm_parser.service;

import com.example.amdm_parser.dto.Song;
import com.example.amdm_parser.utils.TopicCategories;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

@Slf4j
@Service
public class AmDmParser {
    @Value("${parser.songsOnPage}")
    private int songsOnPage;

    public ArrayList<Song> getSongsByCategory(TopicCategories category){
        ArrayList<Song> songsList = new ArrayList<>();
        Document firstPage = getPage(category.getUrl());

        int pagesCount = 1;
        try{
            pagesCount = getPagesCount(firstPage);
        }
        catch (NullPointerException ignored){}
        /*
        Бежим по всем страницам, парсим их и складываем песни в songList
        */
        for (int i = 0; i < pagesCount; i++) {
            Document page = getPage(category.getUrl()+String.format("/page%s", i+1));
            Element songsContainer = getSongsContainer(page);
            songsList.addAll(getSongsListFromContainer(songsContainer, i, category));
        }

        log.info(String.format("Got %s songs by %s, pages count: %s", songsList.size(),
                category.getUrl(), pagesCount));
        return songsList;
    }

    /*
    * Скачивает html страницу по указанной ссылке и отдает как Document
    */
    private Document getPage(String url){
        Document page = new Document(url);
        try {
            page = Jsoup.connect(url).timeout(2*1000).get();
            log.info(String.format("Getting %s...", url));
            return page;
        } catch (IOException e) {
            log.error(String.format("Can't get %s", url));
        }
        return page;
    }

    private int getPagesCount(Document page){
        try{
            Element pagination = page.select("ul.nav-pages").first();
            return pagination.select("li").size();
        }
        catch (Selector.SelectorParseException e){
            return 1;
        }
    }
    /*
    * Вытаскивает элемент-родитель, в котором лежит весь список песен
    */
    private Element getSongsContainer(Document page){
        return page.select("table.items").first();
    }
    /*
    * Вытаскивает из элемента-родителя весь список песен и возвращает как ArrayList
    */
    private ArrayList<Song> getSongsListFromContainer(Element parentContainer, int page, TopicCategories category){
        ArrayList<Song> result = new ArrayList<>();
        Elements songElements = parentContainer.select("td.artist_name");

        for(int i=0; i<songElements.size(); i++){
            try { // Передаем в метод Element (будущий Song), место в списке и категорию
                int songIndex = (page*songsOnPage)+i;
                Song parsedSong = parseElementIntoSong(songElements.get(i), songIndex, category);
                result.add(parsedSong);
            }
            catch (Selector.SelectorParseException e){
                log.error("Can't parse song!");
                e.printStackTrace();
            }
        }
        return result;
    }
    /*
    * Преобразует элемент из родительского элемента с песнями в экземпляр класса Song
    */
    private Song parseElementIntoSong(Element songElement, int index, TopicCategories category){
        String name = songElement.select("a.artist").get(1).text();
        String artist = songElement.select("a.artist").get(0).text();
        String url = songElement.select("a.artist").get(1).attr("href");
        long id = Math.abs(url.hashCode() + category.name().hashCode());
        return new Song(id, url, name, artist, category.name().toLowerCase(Locale.ROOT), index + 1);
    }
}
