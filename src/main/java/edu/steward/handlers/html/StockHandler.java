package edu.steward.handlers.html;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import edu.steward.stock.Stock;
import edu.steward.stock.Fundamentals.DailyChange;
import edu.steward.stock.Fundamentals.Fundamental;
import edu.steward.stock.Fundamentals.Price;
import edu.steward.user.Portfolio;
import edu.steward.user.User;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateViewRoute;

/**
 * Serves up stock page for given ticker, with related stock essentials.
 * 
 * @author wpovell
 */
public class StockHandler implements TemplateViewRoute {
  @Override
  public ModelAndView handle(Request req, Response res) {
    String ticker = req.params(":ticker").toUpperCase();
    ImmutableMap.Builder<Object, Object> variables = new ImmutableMap.Builder<>();
    String user = req.session().attribute("user");
    String id = req.session().attribute("id");
    String pic = req.session().attribute("pic");
    if (user != null) {
      variables.put("user", user);
      variables.put("pic", pic);
      variables.put("id", id);
    }
    variables.put("ticker", ticker);
    Stock stock = new Stock(ticker);

    List<Fundamental> fundamentals = stock.getStockFundamentals();
    if (fundamentals == null) {
      variables.put("title", "Bad Stock");
      return new ModelAndView(variables.build(), "badStock.ftl");
    }

    Map<String, String> funds = new HashMap<>();
    for (Fundamental fund : fundamentals) {
      funds.put(fund.getType(), fund.getNiceValue());
    }

    List<List<String>> ret = new ArrayList<>();
    // Combine Ask and Size
    String p = funds.remove("Ask");
    String s = funds.remove("Ask Size");
    if (p == null || s == null) {
      ret.add(ImmutableList.of("Ask (Size)", "N/A"));
    } else {
      ret.add(ImmutableList.of("Ask (Size)", String.format("%s (%s)", p, s)));
    }
    // Combine Bid and Size
    p = funds.remove("Bid");
    s = funds.remove("Bid Size");
    if (p == null || s == null) {
      ret.add(ImmutableList.of("Bid (Size)", "N/A"));
    } else {
      ret.add(ImmutableList.of("Bid (Size)", String.format("%s (%s)", p, s)));
    }
    // Combine High and Low
    String h = funds.remove("52 Week High");
    String l = funds.remove("52 Week Low");
    ret.add(ImmutableList.of("52 Week", String.format("%s/%s", l, h)));
    // Combine div and yield
    String d = funds.remove("Dividend");
    String y = funds.remove("Yield (%)");
    if (d == null || y == null) {
      ret.add(ImmutableList.of("Dividend & Yield", "N/A"));
    } else {
      ret.add(ImmutableList.of("Dividend & Yield",
          String.format("%s (%s%%)", d, y)));
    }

    for (String key : funds.keySet()) {
      ret.add(ImmutableList.of(key, funds.get(key)));
    }

    Price currPrice = stock.getCurrPrice();
    if (currPrice == null) {
      variables.put("title", "Bad Stock");
      return new ModelAndView(variables.build(), "badStock.ftl");
    }
    DailyChange dailyChange = stock.getDailyChange();
    String company = stock.getCompanyName();
    String color = "up";
    if (dailyChange.getValue() < 0) {
      color = "down";
    }

    User u = new User(id);
    List<String> ports = u.getPortfolios().stream()
        .map((Portfolio mp) -> mp.getName()).collect(Collectors.toList());
    List<String> pools = u.getPoolPorts().stream()
        .map((Portfolio mp) -> mp.getName()).collect(Collectors.toList());

    variables.put("color", color).put("fundamentals", ret)
        .put("price", currPrice).put("change", dailyChange)
        .put("company", company).put("title", "Stock: " + ticker)
        .put("css", "/css/graph.css").put("pools", pools).put("ports", ports);
    return new ModelAndView(variables.build(), "stock.ftl");
  }
}
