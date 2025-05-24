package vciptvman.database;

import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.converters.ConverterException;
import org.sql2o.quirks.NoQuirks;
import org.sql2o.quirks.Quirks;
import vciptvman.model.Bookmark;

import java.sql.SQLException;
import java.util.List;

public class IpTVDatabase {

    private static IpTVDatabase instance;

    public static IpTVDatabase getInstance() {
        if (instance == null) {
            instance = new IpTVDatabase();
            String epgDbPath = System.getProperty("iptv.database");
            String bookmarkDbPath = System.getProperty("bookmark.database");

            if (epgDbPath == null || bookmarkDbPath == null) {
                System.err.println("""
                        Configuration error. Please set the environment variables iptv.database and bookmark.database.
                        Use parameters -Diptv.database=/path/to/iptv.db and -Dbookmark.database=/path/to/bookmark.db.
                        """);
                System.exit(1);
            }

            init(epgDbPath, bookmarkDbPath);
        }
        return instance;
    }

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Fehler beim Laden des JDBC-Treibers");
            e.printStackTrace();
        }
    }

    private static Sql2o sql2oEpg;
    private static Sql2o sql2oBookmark;

    private IpTVDatabase() {
        // disable constructor
    }

    public static void init(String pathToDatabase, String pathToBookmarkDatabase) {
        final Quirks quirks = new NoQuirks() {
            {
                converters.put(String[].class, new StringListConverter());
            }
        };

        sql2oEpg = new Sql2o("jdbc:sqlite:" + pathToDatabase, null, null, quirks);
        sql2oBookmark = new Sql2o("jdbc:sqlite:" + pathToBookmarkDatabase, null, null, quirks);

        // check connection and database
        try (Connection con = sql2oEpg.open()) {
            checkEpgDatabase(con);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try (Connection con = sql2oBookmark.open()) {
            checkBookmarkDatabase(con);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection getEpgConnection() {
        return sql2oEpg.open();
    }

    public static Connection getBookmarkConnection() {
        return sql2oBookmark.open();
    }

    private static void checkEpgDatabase(Connection con) throws SQLException {
        con.createQuery("SELECT * FROM channels").executeAndFetch(String.class);
    }

    private static void checkBookmarkDatabase(Connection con) throws SQLException {
        try {
            con.createQuery("SELECT * FROM bookmarks").executeAndFetch(String.class);
        } catch (Exception e) {
            // table does not exist => create tables
            String sql1 = """
                    CREATE TABLE bookmarks (
                        xmltv_id  TEXT NOT NULL,
                        site      TEXT,
                        site_lang TEXT,
                        url       TEXT,
                        active    BOOLEAN DEFAULT(false),
                        sort_order INTEGER DEFAULT(0),
                    
                        unique (xmltv_id)
                    )
                    """;

            con.createQuery(sql1).executeUpdate();
        }

        // update bookmarks if necessary
        try {
            con.createQuery("ALTER TABLE bookmarks ADD sort_order").executeUpdate();

            // if we reach this point, the column needs some updates
            List<Bookmark> b = con.createQuery("SELECT * FROM bookmarks order by upper(xmltv_id) asc").executeAndFetch(Bookmark.class);

            int i = 1;
            for (Bookmark bookmark : b) {
                con.createQuery("UPDATE bookmarks SET sort_order = :sort_order WHERE xmltv_id = :xmltv_id")
                        .addParameter("xmltv_id", bookmark.xmltv_id())
                        .addParameter("sort_order", i++)
                        .executeUpdate();
            }
        } catch (Exception e) {
            // ignore this, because column possibly exists already
        }
    }

    private static class StringListConverter implements org.sql2o.converters.Converter<String[]> {
        @Override
        public String[] convert(Object val) throws ConverterException {
            if (val instanceof String) {
                return new String[0];
            } else {
                return null;
            }
        }

        @Override
        public Object toDatabaseParam(String[] val) {
            return null;
        }
    }
}
