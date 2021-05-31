package org.polymart.mcplugin;

import org.polymart.mcplugin.utils.JSONWrapper;
import org.polymart.mcplugin.utils.Utils;

public class Resource {

  private final String id;
  private final String title;
  private final String subtitle;
  private final String url;
  private String actualURL;
  private final String price;
  private final String currency;
  private final boolean canDownload;

  public Resource(JSONWrapper json) {
    this.id = json.get("id").asString();
    this.title = json.get("title").asString();
    this.subtitle = json.get("subtitle").asString();
    this.canDownload = json.get("canDownload").asBoolean();
    this.url =
        "https://polymart.org/resource/" + Utils.makeURLFriendlyString(this.title) + "." + this.id;
    this.price = json.get("price").asString();
    this.currency = json.get("currency").asString();
  }

  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getSubtitle() {
    return subtitle;
  }

  public boolean canDownload() {
    return canDownload;
  }

  public String getUrl() {
    return url;
  }

  public String getLongUrl() {
    return actualURL;
  }

  public String getPrice() {
    return price;
  }

  public boolean hasPrice() {
    return price != null && !price.equalsIgnoreCase("0.00");
  }

  public String getCurrency() {
    return currency;
  }

  public boolean isCanDownload() {
    return canDownload;
  }
}
