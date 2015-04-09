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
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BestCostBenefit {
//http://steamcommunity.com/market/priceoverview/?country=BR&currency=7&appid=730&market_hash_name=Chroma%20Case

    public static void main(String[] args) throws MalformedURLException, IOException, ParseException, InterruptedException {
        int[] pages = {10, 11, 12, 14, 15};
        String[] game_ref = {"Team Fortress 2", "Dota 2", "", "CS:GO", ""};
//        int[] pages = {14};
        String tremorLink = "http://www.tremorgames.com/?action=tradeinprices&itemtype=", steamLink = "http://steamcommunity.com/market/search?q=";
        String linha, itemname = "";
        URL tremorUrl;
        URLConnection steamUrl;
        Scanner in, in2;
        Document doc;
        Map<Integer, String> col = new HashMap<>();
        double rel = 0, temp = 0, pnts = 0;
        for (int i = 0; i < pages.length; i++) {
            tremorUrl = new URL(tremorLink + pages[i]); //Monta cada URL
            in = new Scanner(tremorUrl.openStream());//Abre o Scanner
            while (in.hasNext()) {
                if ((linha = in.nextLine()).contains("<table id='prices' class='grid' width='100%'><thead><th style='width:400px;'>")) {
                    doc = Jsoup.parseBodyFragment(linha);
                    Elements item, body = doc.select("tbody").first().children();
                    for (Element body1 : body) {
                        temp = 0;
                        item = body1.select("td");                        
                        pnts = Double.parseDouble(item.get(1).text());
                        steamUrl = new URL(steamLink + URLEncoder.encode(item.get(0).text(), "UTF-8")).openConnection();
                        steamUrl.addRequestProperty("Accept-Language", "pt-BR,en-US;q=0.8,en;q=0.8");
                        steamUrl.addRequestProperty("Cookie", "recentlyVisitedAppHubs=244930; strInventoryLastContext=440_2; sessionid=a7d8731062ced06586588bb8; steamCountry=BR%7C2bbf353507445b9b37908701fc866729; steamLogin=76561198049006380%7C%7C99A07FAD33D0523C1A0047C795423450B1E5DC98; webTradeEligibility=%7B%22allowed%22%3A0%2C%22reason%22%3A2048%2C%22allowed_at_time%22%3A1428365417%2C%22steamguard_required_days%22%3A15%2C%22sales_this_year%22%3A11%2C%22max_sales_per_year%22%3A200%2C%22forms_requested%22%3A0%2C%22new_device_cooldown_days%22%3A7%7D; timezoneOffset=-10800,0; __utma=268881843.1628666419.1427760585.1428020734.1428032257.13; __utmb=268881843.0.10.1428032257; __utmc=268881843; __utmz=268881843.1428020734.12.11.utmcsr=google|utmccn=(organic)|utmcmd=organic|utmctr=(not%20provided)");
                        in2 = new Scanner(steamUrl.getInputStream());
                        while (in2.hasNextLine()) {
                            //Captura a primeira ocorrencia            
                            if ((linha = in2.nextLine()).contains("<a class=\"market_listing_row_link\" href=\"http://steamcommunity.com/market/listings/")) {
                                while (!(linha = in2.nextLine()).equals("</a>")) {
                                    if ((linha = in2.nextLine()).contains("<span style=\"color:white\">")) {
//                                        System.out.println("Item: " + item.get(0).text() + "\tPreço: R$" + linha.substring(linha.lastIndexOf(";") + 2, linha.indexOf("</")));
                                        temp = NumberFormat.getNumberInstance(Locale.FRANCE).parse(linha.substring(linha.lastIndexOf(";") + 2, linha.indexOf("</"))).doubleValue();
                                    }
                                }
                                break;
                            } else if (linha.contains("<div class=\"market_listing_table_message\">")) {
//                                System.out.println("Item não disponível para venda\tItem: " + item.get(0).text());
                                break;
                            }
                        }
                        if (temp > 0) {
                            if (Math.max(rel, (pnts / temp)) == (pnts / temp)) {
                                rel = (pnts / temp);
                                System.out.println("MELHOR CUSTO BENEFÍCIO TEMPORÁRIO:\tItem: " + item.get(0).text() + "\t\tPnt\\R$: " + pnts + "\\" + temp + " = " + rel);
                                itemname = item.get(0).text();
                            }
                        }
                        in2.close();
                    }
                }
            }
            System.out.println("Acabei site " + (i + 1) + "°");
        }
        System.out.println("Best Cost Benefit Item: " + itemname);
    }
}
