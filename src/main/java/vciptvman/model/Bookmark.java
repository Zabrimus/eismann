package vciptvman.model;

public record Bookmark(String xmltv_id, String site, String site_lang, String url, boolean active, int sort_order)
{
}
