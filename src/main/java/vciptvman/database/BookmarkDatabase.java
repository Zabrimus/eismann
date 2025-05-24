package vciptvman.database;

import org.sql2o.Connection;
import vciptvman.model.Bookmark;
import vciptvman.model.EpgSite;

import java.util.List;

public class BookmarkDatabase {
    private final IpTVDatabase database;

    public BookmarkDatabase() {
        database = IpTVDatabase.getInstance();
    }

    public void updateEnabledBookmark(Bookmark bookmark) {
        try (Connection con = database.getBookmarkConnection()) {
            con.createQuery("""
                            INSERT INTO bookmarks (xmltv_id, site, site_lang, url, active, sort_order)
                              VALUES(:xmltv_id, null, null, null, :active, 0)
                              ON CONFLICT(xmltv_id)
                              DO UPDATE SET active = :active
                            """)
                    .addParameter("xmltv_id", bookmark.xmltv_id())
                    .addParameter("active", bookmark.active())
                    .executeUpdate();
        }
    }

    public void updateSiteBookmark(Bookmark bookmark) {
        try (Connection con = database.getBookmarkConnection()) {
            con.createQuery("""
                            INSERT INTO bookmarks (xmltv_id, site, site_lang, url, active, sort_order)
                              VALUES(:xmltv_id, :site, :site_lang, null, false, 0)
                              ON CONFLICT(xmltv_id)
                              DO UPDATE SET site = :site, site_lang = :site_lang
                            """)
                    .addParameter("xmltv_id", bookmark.xmltv_id())
                    .addParameter("site", bookmark.site())
                    .addParameter("site_lang", bookmark.site_lang())
                    .executeUpdate();
        }
    }

    public void updateUrlBookmark(Bookmark bookmark) {
        try (Connection con = database.getBookmarkConnection()) {
            con.createQuery("""
                            INSERT INTO bookmarks (xmltv_id, site, site_lang, url, active, sort_order)
                              VALUES(:xmltv_id, null, null, :url, false, 0)
                              ON CONFLICT(xmltv_id)
                              DO UPDATE SET url = :url
                            """)
                    .addParameter("xmltv_id", bookmark.xmltv_id())
                    .addParameter("url", bookmark.url())
                    .executeUpdate();
        }
    }

    public void updateSortOrderBookmark(Bookmark bookmark) {
        try (Connection con = database.getBookmarkConnection()) {
            con.createQuery("""
                            INSERT INTO bookmarks (xmltv_id, site, site_lang, url, active, sort_order)
                              VALUES(:xmltv_id, null, null, null, false, :sort_order)
                              ON CONFLICT(xmltv_id)
                              DO UPDATE SET sort_order = :sort_order
                            """)
                    .addParameter("xmltv_id", bookmark.xmltv_id())
                    .addParameter("sort_order", bookmark.sort_order())
                    .executeUpdate();
        }
    }

    public EpgSite getSite(String xmltv_id) {
        try (Connection con = database.getBookmarkConnection()) {
            return con.createQuery("SELECT site, site_lang FROM bookmarks WHERE xmltv_id = :xmltv_id")
                    .addParameter("xmltv_id", xmltv_id)
                    .executeAndFetchFirst(EpgSite.class);
        }
    }

    public boolean getActive(String xmltv_id) {
        try (Connection con = database.getBookmarkConnection()) {
            String result = con.createQuery("SELECT active FROM bookmarks WHERE xmltv_id = :xmltv_id")
                    .addParameter("xmltv_id", xmltv_id)
                    .executeAndFetchFirst(String.class);

            return result != null && !result.isEmpty() && !result.equals("0");
        }
    }

    public String getUrl(String xmltv_id) {
        try (Connection con = database.getBookmarkConnection()) {
            return con.createQuery("SELECT url FROM bookmarks WHERE xmltv_id = :xmltv_id")
                    .addParameter("xmltv_id", xmltv_id)
                    .executeAndFetchFirst(String.class);
        }
    }

    public List<Bookmark> getAllBookmarks() {
        try (Connection con = database.getBookmarkConnection()) {
            return con.createQuery("SELECT * FROM bookmarks order by sort_order asc")
                    .executeAndFetch(Bookmark.class);
        }
    }

    public void delete(String xmltv_id) {
        try (Connection con = database.getBookmarkConnection()) {
            con.createQuery("DELETE from bookmarks WHERE xmltv_id = :xmltv_id")
               .addParameter("xmltv_id", xmltv_id)
               .executeUpdate();
        }
    }
}
