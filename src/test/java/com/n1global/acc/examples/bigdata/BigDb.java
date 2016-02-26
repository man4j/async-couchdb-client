package com.n1global.acc.examples.bigdata;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

import com.n1global.acc.CouchDb;
import com.n1global.acc.CouchDbConfig;
import com.n1global.acc.json.CouchDbDocument;
import com.ning.http.client.AsyncHttpClient;

public class BigDb extends CouchDb {
    public static class User extends CouchDbDocument {
        private String name;

        private int age;

        public User() {
        }

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    public BigDb(CouchDbConfig config) {
        super(config);
    }

    @SuppressWarnings("unchecked")
    private static void insert(BigDb db) throws Exception {
        long t = System.currentTimeMillis();

        List<User> users = new ArrayList<>(5_000);

        for (int i = 1; i <= 1_000_000; i++) {
            users.add(new User("User name", 21));

            if (users.size() == 5_000) {
                Future<User[]>[] futures = new Future[] {
                    db.async().saveOrUpdate(users.subList(     0,    1_000)),
                    db.async().saveOrUpdate(users.subList( 1_000,    2_000)),
                    db.async().saveOrUpdate(users.subList( 2_000,    3_000)),
                    db.async().saveOrUpdate(users.subList( 3_000,    4_000)),
                    db.async().saveOrUpdate(users.subList( 4_000,    5_000))};

                for (Future<User[]> f : futures) {
                    f.get();
                }

                users.clear();
            }
        }

        System.out.println("Insert complete: " + (System.currentTimeMillis() - t));
    }

    @SuppressWarnings("unchecked")
    private static void insert2(final BigDb db) throws Exception {
        long t = System.currentTimeMillis();

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    List<User> users = new ArrayList<>(10_000);

                    for (int i = 1; i <= 4_000_000; i++) {
                        users.add(new User("User name", 21));

                        if (users.size() == 10_000) {
                            Future<User[]>[] futures = new Future[] {
                                db.async().saveOrUpdate(users)};

                            for (Future<User[]> f : futures) {
                                try {
                                    f.get();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            users.clear();
                        }
                    }
                }
            };

            threads.add(thread);

            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("Insert complete: " + (System.currentTimeMillis() - t));
    }

    @SuppressWarnings("unused")
    private static void read(BigDb db) {
        int count = 0;

        long t = System.currentTimeMillis();

        for (CouchDbDocument d : db.getBuiltInView().createDocQuery().asDocIterator(10000)) {
            count++;
        }

        System.out.println("Read complete. Count: " + count + " Time: " + (System.currentTimeMillis() - t));

    }

    public static void main(String[] args) throws Exception {
        loggerOff();

        AsyncHttpClient httpClient = new AsyncHttpClient();

        BigDb db = new BigDb(new CouchDbConfig.Builder().setUser("admin")
                                                        .setPassword("root")
                                                        .setHttpClient(httpClient)
                                                        .build());

        //insert(db);
            insert2(db);

//        read(db);

        httpClient.close();
    }

    private static void loggerOff() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        context.getLogger("com").setLevel(Level.ERROR);
    }
}
