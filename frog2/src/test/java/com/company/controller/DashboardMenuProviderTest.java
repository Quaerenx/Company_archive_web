package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DashboardMenuProviderTest {
    @Test
    void preservesMenuOrderTitlesAndCanonicalUrls() {
        Map<String, List<DashboardServlet.MenuItem>> menus =
                DashboardMenuProvider.build();

        assertEquals(List.of("고객 관리", "자료 관리"),
                new ArrayList<>(menus.keySet()));
        assertEquals(
                List.of("customers?view=list", "maintenance"),
                menus.get("고객 관리").stream()
                        .map(DashboardServlet.MenuItem::getUrl)
                        .toList());
        assertEquals(
                List.of(
                        "meeting?view=list",
                        "file-repository",
                        "troubleshooting?view=list"),
                menus.get("자료 관리").stream()
                        .map(DashboardServlet.MenuItem::getUrl)
                        .toList());
    }
}
