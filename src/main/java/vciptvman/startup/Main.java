package vciptvman.startup;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import vciptvman.database.IpTVDatabase;

@QuarkusMain
public class Main {
    public static void main(String... args) {
        Quarkus.run(MyApp.class, args);
    }

    public static class MyApp implements QuarkusApplication {
        @Override
        public int run(String... args) throws Exception {
            // initialize Singelton as early as possible
            IpTVDatabase.getInstance();

            Quarkus.waitForExit();
            return 0;
        }
    }
}