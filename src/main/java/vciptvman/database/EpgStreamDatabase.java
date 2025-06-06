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
                        SELECT * FROM (
                            SELECT DISTINCT s.xmltv_id xmltv_id, s.name name, co.name country, co.code country_code, co.flag flag, ch.categories categories, ch.website website, ch.logo logo 
                            FROM streams s
                            LEFT JOIN countries co ON co.code = s.country
                            LEFT JOIN channels ch ON ch.id = s.ref_channel_id                            
                        
                            UNION
                        
                            SELECT DISTINCT s.xmltv_id xmltv_id, s.name name, co.name country, co.code country_code, co.flag flag, ch.categories categories, ch.website website, ch.logo logo 
                            FROM epg_channels s
                            LEFT JOIN countries co ON co.code = s.country
                            LEFT JOIN channels ch ON ch.id = s.ref_channel_id                            
                        )                        
                        GROUP BY xmltv_id
                        ORDER BY upper(name);
                    """)
                    .executeAndFetch(Stream.class);
        }
    }

    public List<Stream> getAllStreamsWithSite(String site) {
        try (Connection con = database.getEpgConnection()) {
            return con.createQuery("""
                        SELECT * FROM (
                                SELECT DISTINCT s.xmltv_id xmltv_id, s.name name, co.name country, co.code country_code, co.flag flag, ch.categories categories, ch.website website, ch.logo logo
                                      FROM streams s
                                              LEFT JOIN countries co ON co.code = s.country
                                              LEFT JOIN channels ch ON ch.id = s.ref_channel_id
                    
                                              UNION
                    
                                              SELECT DISTINCT s.xmltv_id xmltv_id, s.name name, co.name country, co.code country_code, co.flag flag, ch.categories categories, ch.website website, ch.logo logo
                                      FROM epg_channels s
                                        LEFT JOIN countries co ON co.code = s.country
                                        LEFT JOIN channels ch ON ch.id = s.ref_channel_id
                        ) a
                        WHERE (SELECT count(*) FROM epg_channels e WHERE a.xmltv_id = e.xmltv_id AND site = :site) > 0
                        GROUP BY xmltv_id
                        ORDER by upper(name);
                    """)
                    .addParameter("site", site)
                    .executeAndFetch(Stream.class);
        }
    }

    public List<Stream> getBookmarkedStreams() {
        BookmarkDatabase bookmarkDatabase = new BookmarkDatabase();

        List<Stream> allStreams = getAllStreams();
        List<Bookmark> bookmarks = bookmarkDatabase.getAllBookmarks();

        List<Stream> result = new ArrayList<>();

        Map<String, Stream> streamMap = new HashMap<>();
        allStreams.stream().forEach(stream -> streamMap.put(stream.xmltv_id(), stream));

        bookmarks.stream().forEach(bookmark -> {
            Stream r = streamMap.get(bookmark.xmltv_id());
            if (r != null) {
                result.add(r);
            } else {
                Stream n = new Stream(bookmark.xmltv_id(), "Invalid: " + bookmark.xmltv_id(), null, null, null, null, null, null );
                result.add(n);
            }
        });

        // filter streams
        return result;
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
            List<EpgSite> result = con.createQuery("""
                            SELECT site, lang site_lang 
                            FROM epg_channels 
                            WHERE xmltv_id = :id                        
                        """)
                    .addParameter("id", id)
                    .executeAndFetch(EpgSite.class);

            result.sort(Comparator.comparing(EpgSite::site));
            return result;
        }
    }

    public boolean hasProviderXmltvId(String xmltv_id, String provider) {
        try (Connection con = database.getEpgConnection()) {
            int result = con.createQuery("""
                            SELECT count(*) FROM epg_channels
                            WHERE site = :site
                            AND xmltv_id = :xmltv_id
                          """)
                    .addParameter("site", provider)
                    .addParameter("xmltv_id", xmltv_id)
                    .executeScalar(Integer.class);

            return result > 0;
        }
    }

    public List<StreamUrl> getStreamUrls(String id) {
        try (Connection con = database.getEpgConnection()) {
            List<StreamUrl> result = con.createQuery("""
                            SELECT s.name, s.url
                            FROM streams s
                            WHERE s.xmltv_id = :id
                        """)
                    .addParameter("id", id)
                    .executeAndFetch(StreamUrl.class);

            return result.stream().filter(streamUrl -> streamUrl.url() != null && !streamUrl.url().isEmpty()).toList();
        }
    }

    public String getName(String xmltv_id) {
        try (Connection con = database.getEpgConnection()) {
            return con.createQuery("""
                            SELECT distinct name FROM epg_channels WHERE xmltv_id = :xmltv_id
                            """)
                    .addParameter("xmltv_id", xmltv_id)
                    .executeAndFetchFirst(String.class);
        }
    }

    public String getSiteId(String xmltv_id, String site, String lang) {
        try (Connection con = database.getEpgConnection()) {
            return con.createQuery("""
                            SELECT site_id from epg_channels WHERE xmltv_id = :xmltv_id and site = :site and lang = :lang
                          """)
                    .addParameter("xmltv_id", xmltv_id)
                    .addParameter("site", site)
                    .addParameter("lang", lang)
                    .executeAndFetchFirst(String.class);
        }
    }

    public List<String> getAllEpgSites() {
        try (Connection con = database.getEpgConnection()) {
            return con.createQuery("""
                            SELECT DISTINCT site
                            FROM epg_channels
                            ORDER BY site;
                          """)
                    .executeAndFetch(String.class);
        }
    }

}
