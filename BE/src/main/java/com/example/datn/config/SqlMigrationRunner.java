package com.example.datn.config;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Tu chay cac file SQL trong classpath:db/migration/*.sql khi khoi dong ung dung.
 * Thu tu: theo ten file (A-Z). File da chay se duoc ghi vao bang dbo.__chay_sql
 * va khong chay lai. File loi se dung khoi dong (fail-fast).
 */
@Component
public class SqlMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SqlMigrationRunner.class);
    private static final String BANG = "dbo.__chay_sql";

    private final DataSource dataSource;

    public SqlMigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver()
            .getResources("classpath*:db/migration/*.sql");
        if (resources.length == 0) {
            log.info("Khong tim thay file SQL migration nao");
            return;
        }
        Arrays.sort(resources, Comparator.comparing(r -> String.valueOf(r.getFilename())));
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute(
                "IF OBJECT_ID('" + BANG + "', 'U') IS NULL CREATE TABLE " + BANG + " ("
                    + "ten_file NVARCHAR(255) NOT NULL PRIMARY KEY, "
                    + "ap_luc DATETIME2 NOT NULL DEFAULT SYSDATETIME())"
            );
            Set<String> daChay = new HashSet<>();
            try (ResultSet rs = st.executeQuery("SELECT ten_file FROM " + BANG)) {
                while (rs.next()) {
                    daChay.add(rs.getString(1));
                }
            }
            int moi = 0;
            for (Resource r : resources) {
                String ten = r.getFilename();
                if (ten == null || daChay.contains(ten)) {
                    continue;
                }
                long batDau = System.currentTimeMillis();
                st.execute(doc(r));
                try (Statement st2 = conn.createStatement()) {
                    st2.execute(
                        "INSERT INTO " + BANG + " (ten_file) VALUES (N'"
                            + ten.replace("'", "''") + "')"
                    );
                }
                moi++;
                log.info(
                    "Da chay SQL migration {} ({} ms)",
                    ten,
                    System.currentTimeMillis() - batDau
                );
            }
            if (moi == 0) {
                log.info("Cac SQL migration da duoc chay du day ({} file)", resources.length);
            }
        }
    }

    private String doc(Resource r) throws Exception {
        try (InputStream in = r.getInputStream()) {
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (!sql.isEmpty() && sql.charAt(0) == '\uFEFF') {
                sql = sql.substring(1);
            }
            return sql;
        }
    }
}
