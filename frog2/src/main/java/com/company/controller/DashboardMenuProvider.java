package com.company.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DashboardMenuProvider {
    private DashboardMenuProvider() {
    }

    static Map<String, List<DashboardServlet.MenuItem>> build() {
        Map<String, List<DashboardServlet.MenuItem>> menus = new LinkedHashMap<>();
        menus.put("고객 관리", List.of(
                new DashboardServlet.MenuItem(
                        "고객 정보", "customers?view=list", "fas fa-address-card"),
                new DashboardServlet.MenuItem(
                        "정기점검 이력", "maintenance", "fas fa-clipboard-check")));
        menus.put("자료 관리", List.of(
                new DashboardServlet.MenuItem(
                        "회의록", "meeting?view=list", "fas fa-users"),
                new DashboardServlet.MenuItem(
                        "자료실", "file-repository", "fas fa-file-alt"),
                new DashboardServlet.MenuItem(
                        "트러블슈팅", "troubleshooting?view=list", "fas fa-tools")));
        return menus;
    }
}
