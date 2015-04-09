/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javaapplication2;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Marcos
 */
public class SearchWishlist {

    public static void main(String[] args) throws MalformedURLException, IOException {
        Map<Integer, String> gamesPrices = new TreeMap<>();
        Document doc = Jsoup.connect("http://www.steamcommunity.com/profiles/76561198049006380/wishlist/?sort=added")
                .header("Accept-Language", "pt-BR,en-US;q=0.8,en;q=0.8")
                .header("Cookie", "recentlyVisitedAppHubs=244930; strInventoryLastContext=440_2; sessionid=a7d8731062ced06586588bb8; steamCountry=BR%7C2bbf353507445b9b37908701fc866729; steamLogin=76561198049006380%7C%7C99A07FAD33D0523C1A0047C795423450B1E5DC98; webTradeEligibility=%7B%22allowed%22%3A0%2C%22reason%22%3A2048%2C%22allowed_at_time%22%3A1428365417%2C%22steamguard_required_days%22%3A15%2C%22sales_this_year%22%3A11%2C%22max_sales_per_year%22%3A200%2C%22forms_requested%22%3A0%2C%22new_device_cooldown_days%22%3A7%7D; timezoneOffset=-10800,0; __utma=268881843.1628666419.1427760585.1428020734.1428032257.13; __utmb=268881843.0.10.1428032257; __utmc=268881843; __utmz=268881843.1428020734.12.11.utmcsr=google|utmccn=(organic)|utmcmd=organic|utmctr=(not%20provided)")
                .get();
        Elements games, item = doc.select("h4");
        String itemPoints;
        for (Element i : item) {
            doc = Jsoup.connect("http://www.tremorgames.com/index.php?action=shop&search_category=5&searchterm=" + URLEncoder.encode(i.text(), "UTF-8"))
                    .header("Accept-Language", "pt-BR,en-US;q=0.8,en;q=0.8")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.118 Safari/537.36")
                    .header("Accept-Encoding", "gzip, deflate, sdch")
                    .header("Cookie", "uid=1259204; PHPSESSID=49ac2bfc2177b7c42d56435d479dbd5d; __utmt=1; __utma=269348916.974876551.1427771853.1428398700.1428590105.29; __utmb=269348916.1.10.1428590105; __utmc=269348916; __utmz=269348916.1427777064.3.2.utmcsr=google|utmccn=(organic)|utmcmd=organic|utmctr=(not%20provided); tguserid=1259204; eccheck=1")
                    .get();
            games = doc.select("div.shop_item_box");
            System.out.println(i.text() + "\t\t\t" + games.size());
            int points = Integer.MAX_VALUE;
            for (Element game : games) {
                itemPoints = game.select("div.shop_item_box_type").get(1).text();
                if (game.text().contains(i.text() + " Steam Game") || game.text().contains(i.text() + " Steam Gift")) {
                    points = Math.min(points, Integer.parseInt(itemPoints.substring(0, itemPoints.indexOf("T") - 1)));
                }
            }
            if (points > 0) {
                if (gamesPrices.containsKey(points)) {
                    gamesPrices.put(points, gamesPrices.get(points) + " , " + i.text());
                } else {
                    gamesPrices.put(points, i.text());
                }
            }
        }
        int partialValue = 0;
        for (Map.Entry<Integer, String> entrySet : gamesPrices.entrySet()) {
            Integer key = entrySet.getKey();
            String value = entrySet.getValue();
            partialValue += key * value.split(",").length;
            System.out.println("Partial Value: " + partialValue + "\tPoints:\t" + key + "\t\tGames = {" + value + "}");
        }
    }
}
