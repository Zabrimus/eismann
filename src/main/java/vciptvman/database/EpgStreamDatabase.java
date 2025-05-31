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
                                    select distinct IIF(instr(x.xmltv_id2, '@') = 0, x.xmltv_id2, x.xmltv_id) xmltv_id,
                                                    x.name name,
                                                    co.name country,
                                                    co.code country_code,
                                                    co.flag,
                                                    coalesce(coalesce(c.categories, c2.categories), c3.categories) categories,
                                                    coalesce(coalesce(c.website, c2.website), c3.website) website,
                                                    coalesce(coalesce(c.logo, c2.logo), c3.logo) logo
                                    from streams s, xmltvid x, countries co
                                    left join channels c on c.ref_xmltvid = x.id
                                    left join channels c2 on c2.ref_xmltvid = (
                                        select id
                                        from xmltvid x2
                                        where x2.xmltv_id = SUBSTR(x.xmltv_id, 1, INSTR(x.xmltv_id, '@') - 1) and x.xmltv_id like '%@%'
                                    )
                                    left join channels c3 on c3.ref_xmltvid = (
                                        select id
                                        from xmltvid x3
                                        where x3.xmltv_id = SUBSTR(x.xmltv_id2, 1, INSTR(x.xmltv_id2, '@') - 1) and x.xmltv_id2 like '%@%'
                                    )
                                    where s.ref_xmltvid = x.id
                                      and x.country = co.code
                    
                                    union
                    
                                    select distinct IIF(instr(x.xmltv_id2, '@') = 0, x.xmltv_id2, x.xmltv_id) xmltv_id,
                                                    x.name,
                                                    co.name country,
                                                    co.code country_code,
                                                    co.flag,
                                                    coalesce(coalesce(c.categories, c2.categories), c3.categories) categories,
                                                    coalesce(coalesce(c.website, c2.website), c3.website) website,
                                                    coalesce(coalesce(c.logo, c2.logo), c3.logo) logo
                                    from epg_channels e, xmltvid x
                                    left join countries co on x.country = co.code
                                    left join channels c on c.ref_xmltvid = x.id
                                    left join channels c2 on c2.ref_xmltvid = (
                                        select id
                                        from xmltvid x2
                                        where x2.xmltv_id = SUBSTR(x.xmltv_id, 1, INSTR(x.xmltv_id, '@') - 1)
                                          and  x.xmltv_id like '%@%'
                                    )
                                    left join channels c3 on c3.ref_xmltvid = (
                                        select id
                                        from xmltvid x3
                                        where x3.xmltv_id = SUBSTR(x.xmltv_id2, 1, INSTR(x.xmltv_id2, '@') - 1) and x.xmltv_id2 like '%@%'
                                    )
                                    where e.ref_xmltvid = x.id
                    """)
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
                            FROM epg_channels e
                            WHERE e.ref_xmltvid = (
                                    SELECT id
                                    FROM xmltvid x
                                    WHERE x.xmltv_id = :id
                                    OR x.xmltv_id2 = :id)
                        """)
                    .addParameter("id", id)
                    .executeAndFetch(EpgSite.class);

            result.sort(Comparator.comparing(EpgSite::site));
            return result;
        }
    }

    public List<StreamUrl> getStreamUrls(String id) {
        try (Connection con = database.getEpgConnection()) {
            List<StreamUrl> result = con.createQuery("""
                            SELECT x.name, s.url 
                            FROM xmltvid x
                            LEFT JOIN streams s ON s.ref_xmltvid = x.id
                            WHERE x.xmltv_id = :id
                            OR    x.xmltv_id2 = :id
                    """)
                    .addParameter("id", id)
                    .executeAndFetch(StreamUrl.class);

            return result.stream().filter(streamUrl -> streamUrl.url() != null && !streamUrl.url().isEmpty()).toList();
        }
    }

    public String getName(String xmltv_id) {
        try (Connection con = database.getEpgConnection()) {
            return con.createQuery("""
                            SELECT x.name 
                            FROM xmltvid x
                            LEFT JOIN channels c on x.id = c.ref_xmltvid
                            WHERE x.xmltv_id = :id OR x.xmltv_id2 = :id;
                            """)
                    .addParameter("id", xmltv_id)
                    .executeAndFetchFirst(String.class);
        }
    }

    public String getSiteId(String xmltv_id, String site, String lang) {
        try (Connection con = database.getEpgConnection()) {
            return con.createQuery("""
                            SELECT site_id 
                            FROM xmltvid x
                            LEFT JOIN epg_channels c ON x.id = c.ref_xmltvid AND c.site = :site
                            where x.xmltv_id = :xmltv_id OR x.xmltv_id2 = :xmltv_id;
                          """)
                    .addParameter("xmltv_id", xmltv_id)
                    .addParameter("site", site)
                    .executeAndFetchFirst(String.class);
        }
    }

}
