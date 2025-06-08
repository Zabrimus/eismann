package vciptvman.database;

import org.sql2o.Connection;
import org.sql2o.Query;
import vciptvman.model.*;
import vciptvman.vdr.VdrClient;
import vciptvman.view.Bookmarks;

import java.io.IOException;
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

    public void deleteBookmark(String xmltv_id) {
        try (Connection con = database.getBookmarkConnection()) {
            con.createQuery("DELETE from bookmarks WHERE xmltv_id = :xmltv_id")
               .addParameter("xmltv_id", xmltv_id)
               .executeUpdate();
        }
    }

    public void deleteVdr(String id) {
        try (Connection con = database.getBookmarkConnection()) {
            con.createQuery("DELETE from vdr_channels WHERE channel_id = :channel_id")
                    .addParameter("channel_id", id)
                    .executeUpdate();
        }
    }

    public List<VdrChannel> getVdrChannels() {
        try (Connection con = database.getBookmarkConnection()) {
            return con.createQuery("SELECT * from vdr_channels")
                      .executeAndFetch(VdrChannel.class);
        }
    }

    public void readVdrChannels(String host, int port) throws IOException {
        VdrClient client = new VdrClient(host, port);
        List<VdrChannel> channels = client.getChannels();

        try (Connection con = database.getBookmarkConnection()) {
            String sql1 = "UPDATE vdr_channels SET STATUS = -1";
            con.createQuery(sql1).executeUpdate();

            String sql2 = """
                        INSERT INTO vdr_channels (channel_id, name, status) values (:channel_id, :name, :status)
                        ON CONFLICT(channel_id)
                        DO UPDATE SET status = :status
                    """;

            Query query = con.createQuery(sql2);
            for(VdrChannel channel : channels) {
                query.addParameter("channel_id", channel.channel_id())
                        .addParameter("name", channel.name())
                        .addParameter("status", 1)
                        .executeUpdate();
            }
        }
    }

    public void setVdrChannel(Stream stream, VdrChannel vdr, VdrChannel oldVdr) {
        try (Connection con = database.getBookmarkConnection()) {
            if (vdr == null && oldVdr != null) {
                // delete values
                String sql = "UPDATE vdr_channels SET xmltv_id = null, site = null, site_lang = null WHERE channel_id = :channel_id";
                con.createQuery(sql)
                        .addParameter("channel_id", oldVdr.channel_id())
                        .executeUpdate();
            } else if (vdr != null && oldVdr == null) {
                String sql1 = "SELECT * from bookmarks WHERE xmltv_id = :xmltv_id";
                Bookmark bm = con.createQuery(sql1)
                                .addParameter("xmltv_id", stream.xmltv_id())
                                .executeAndFetchFirst(Bookmark.class);

                String sql2 = "UPDATE vdr_channels SET xmltv_id = :xmltv_id WHERE channel_id = :channel_id";
                con.createQuery(sql2)
                        .addParameter("xmltv_id", stream.xmltv_id())
                        .addParameter("channel_id", vdr.channel_id())
                        .executeUpdate();

                String sql3 = "UPDATE vdr_channels SET site = :site, site_lang = :site_lang WHERE channel_id = :channel_id";
                con.createQuery(sql3)
                        .addParameter("site", bm.site())
                        .addParameter("site_lang", bm.site_lang())
                        .addParameter("channel_id", vdr.channel_id())
                        .executeUpdate();
            } else if (vdr == null && oldVdr == null) {
                String sql1 = "SELECT * from bookmarks WHERE xmltv_id = :xmltv_id";
                Bookmark bm = con.createQuery(sql1)
                        .addParameter("xmltv_id", stream.xmltv_id())
                        .executeAndFetchFirst(Bookmark.class);

                String sql3 = "UPDATE vdr_channels SET site = :site, site_lang = :site_lang WHERE xmltv_id = :xmltv_id";
                con.createQuery(sql3)
                        .addParameter("site", bm.site())
                        .addParameter("site_lang", bm.site_lang())
                        .addParameter("xmltv_id", stream.xmltv_id())
                        .executeUpdate();
            }
        }
    }

    public void setVdrChannel(VdrChannel vdr, OtherEpgProvider newValue) {
        try (Connection con = database.getBookmarkConnection()) {
                // set value
            String sql = "UPDATE vdr_channels SET other_id = :other_id WHERE channel_id = :channel_id";
            con.createQuery(sql)
                    .addParameter("other_id", newValue != null ? newValue.name() : null)
                    .addParameter("channel_id", vdr.channel_id())
                    .executeUpdate();
        }
    }
}
