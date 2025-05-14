package vciptvman.database;

import org.sql2o.Connection;
import vciptvman.model.*;

import java.util.*;

public class EpgStreamDatabase {
    private final IpTVDatabase database;

    public EpgStreamDatabase() {
        database = IpTVDatabase.getInstance();
    }

    public Map<String, String> getAllCategories() {
        Map<String, String> categories = new HashMap<>();

        try (Connection con = database.getEpgConnection()) {
            con.createQuery("SELECT * FROM categories order by name")
                    .executeAndFetch(Category.class).stream().forEach(e -> {categories.put(e.id(), e.name());});
            return categories;
        }
    }

    public List<Stream> getAllStreams() {
        try (Connection con = database.getEpgConnection()) {
            return con.createQuery("""
                        select * from (
                            SELECT DISTINCT s.tvgid xmltv_id,
                                           c.name,
                                           co.name country,
                                           co.code country_code,
                                           co.flag,
                                           c.categories,
                                           c.website,
                                           c.logo
                           FROM streams s,
                                channels c,
                                countries co
                           WHERE s.tvgid = c.id
                             AND c.country = co.code
                    
                           UNION
                    
                           SELECT DISTINCT ep.xmltv_id,
                                           c.name,
                                           co.name country,
                                           co.code country_code,
                                           co.flag,
                                           c.categories,
                                           c.website,
                                           c.logo
                           FROM epg_channels ep
                                    LEFT JOIN channels c on c.id = ep.xmltv_id
                                    LEFT JOIN streams s on s.tvgid = ep.xmltv_id
                                    LEFT JOIN countries co on c.country = co.code
                           WHERE ep.xmltv_id = c.id
                             AND ep.xmltv_id IS NOT NULL
                        )
                        order by upper(name)
                    """)
                    .executeAndFetch(Stream.class);
        }
    }

    public List<Stream> getBookmarkedStreams() {
        BookmarkDatabase bookmarkDatabase = new BookmarkDatabase();

        List<Stream> allStreams = getAllStreams();
        List<Bookmark> bookmarks = bookmarkDatabase.getAllBookmarks();

        // create set of xmltv_ids
        Set<String> xmltv_ids = new HashSet<>();
        bookmarks.stream().map(Bookmark::xmltv_id).forEach(xmltv_ids::add);

        // filter streams
        return allStreams.stream().filter(stream -> xmltv_ids.contains(stream.xmltv_id())).toList();
    }

    public Country getCountry(String code) {
        try (Connection con = database.getEpgConnection()) {
            return con.createQuery("SELECT * FROM countries WHERE code = :code")
                    .addParameter("code", code)
                    .executeAndFetchFirst(Country.class);
        }
    }

    public List<EpgSite> getEpgProviderById(String id) {
        try (Connection con = database.getEpgConnection()) {
            List<EpgSite> result = con.createQuery("SELECT site, lang site_lang FROM epg_channels WHERE xmltv_id = :id")
                    .addParameter("id", id)
                    .executeAndFetch(EpgSite.class);

            result.sort(Comparator.comparing(EpgSite::site));
            return result;
        }
    }

    public List<StreamUrl> getStreamUrls(String id) {
        try (Connection con = database.getEpgConnection()) {
            return con.createQuery("SELECT name, url FROM streams WHERE tvgid = :id")
                    .addParameter("id", id)
                    .executeAndFetch(StreamUrl.class);
        }
    }

    public String getName(String xmltv_id) {
        try (Connection con = database.getEpgConnection()) {
            return con.createQuery("SELECT name FROM channels WHERE id = :xmltv_id")
                    .addParameter("xmltv_id", xmltv_id)
                    .executeAndFetchFirst(String.class);
        }
    }

    public String getSiteId(String xmltv_id, String site, String lang) {
        try (Connection con = database.getEpgConnection()) {
            return con.createQuery("""
                        SELECT site_id FROM epg_channels 
                         WHERE lang = :lang
                           AND xmltv_id = :xmltv_id
                           AND site = :site
                      """)
                    .addParameter("xmltv_id", xmltv_id)
                    .addParameter("site", site)
                    .addParameter("lang", lang)
                    .executeAndFetchFirst(String.class);
        }
    }

}
