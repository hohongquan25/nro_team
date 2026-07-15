package nro.models.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;

public class AdminWebServer {
    private HttpServer server;

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/", new UIHandler());
            server.createContext("/api/add-item", new AddItemHandler());
            server.createContext("/api/add-money", new AddMoneyHandler());
            server.createContext("/api/register", new RegisterHandler());
            server.createContext("/api/items", new ItemsHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("Admin Web Server started on port 8080. Truy cập http://localhost:8080 để sử dụng.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class UIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <title>NRO Admin Panel - Thêm Vật Phẩm</title>" +
                "    <style>" +
                "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 20px; background: #2c3e50; color: #333; display: flex; justify-content: center; gap: 20px; flex-wrap: wrap; }" +
                "        .container { background: white; padding: 30px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.2); width: 350px; margin-top: 40px; }" +
                "        h2 { text-align: center; color: #2c3e50; margin-top: 0; }" +
                "        .form-group { margin-bottom: 15px; }" +
                "        label { display: block; margin-bottom: 5px; font-weight: bold; color: #34495e; }" +
                "        input { width: 100%; padding: 10px; border: 1px solid #bdc3c7; border-radius: 6px; box-sizing: border-box; font-size: 14px; }" +
                "        input:focus { border-color: #3498db; outline: none; }" +
                "        button { width: 100%; background: #3498db; color: white; border: none; padding: 12px; cursor: pointer; font-size: 16px; border-radius: 6px; font-weight: bold; transition: background 0.3s; }" +
                "        button:hover { background: #2980b9; }" +
                "        .btn-reg { background: #e67e22; }" +
                "        .btn-reg:hover { background: #d35400; }" +
                "        .result { margin-top: 20px; text-align: center; font-weight: bold; padding: 10px; border-radius: 6px; display: none; }" +
                "        .success { background: #d4edda; color: #155724; display: block; }" +
                "        .error { background: #f8d7da; color: #721c24; display: block; }" +
                "        .item-list { max-height: 150px; overflow-y: auto; border: 1px solid #bdc3c7; border-radius: 6px; margin-top: 5px; display: none; background: #f9f9f9; }" +
                "        .item-entry { padding: 8px; cursor: pointer; border-bottom: 1px solid #eee; font-size: 13px; }" +
                "        .item-entry:hover { background: #e0f7fa; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class=\"container\">" +
                "        <h2>Thêm Vật Phẩm</h2>" +
                "        <div class=\"form-group\">" +
                "            <label>Tên nhân vật đang Online:</label>" +
                "            <input type=\"text\" id=\"player\" placeholder=\"Ví dụ: admin1\">" +
                "        </div>" +
                "        <div class=\"form-group\">" +
                "            <label>Tìm vật phẩm:</label>" +
                "            <input type=\"text\" id=\"searchBox\" onkeyup=\"filterItems()\" placeholder=\"Gõ tên để tìm (VD: Cải trang)\">" +
                "            <div id=\"itemList\" class=\"item-list\"></div>" +
                "        </div>" +
                "        <div class=\"form-group\">" +
                "            <label>ID Vật phẩm (Hoặc chọn ở trên):</label>" +
                "            <input type=\"number\" id=\"itemId\" placeholder=\"Ví dụ: 457\">" +
                "        </div>" +
                "        <div class=\"form-group\">" +
                "            <label>Số lượng:</label>" +
                "            <input type=\"number\" id=\"quantity\" placeholder=\"Số lượng\" value=\"1\">" +
                "        </div>" +
                "        <div class=\"form-group\">" +
                "            <label>Chỉ số (Tuỳ chọn. VD: 50:10; 77:20):</label>" +
                "            <input type=\"text\" id=\"options\" placeholder=\"id_option:chỉ_số (Cách nhau bằng dấu ;)\">" +
                "        </div>" +
                "        <button onclick=\"addItem()\">Thêm Vào Hành Trang</button>" +
                "        <div id=\"resultItem\" class=\"result\"></div>" +
                "    </div>" +
                "    <div class=\"container\">" +
                "        <h2>Đăng Ký Tài Khoản</h2>" +
                "        <div class=\"form-group\">" +
                "            <label>Tài khoản:</label>" +
                "            <input type=\"text\" id=\"regUser\" placeholder=\"Tài khoản đăng nhập\">" +
                "        </div>" +
                "        <div class=\"form-group\">" +
                "            <label>Mật khẩu:</label>" +
                "            <input type=\"password\" id=\"regPass\" placeholder=\"Mật khẩu\">" +
                "        </div>" +
                "        <button class=\"btn-reg\" onclick=\"register()\">Đăng Ký Ngay</button>" +
                "        <div id=\"resultReg\" class=\"result\"></div>" +
                "    </div>" +
                "    <div class=\"container\">" +
                "        <h2>Cộng Vàng / Ngọc</h2>" +
                "        <div class=\"form-group\">" +
                "            <label>Tên nhân vật đang Online:</label>" +
                "            <input type=\"text\" id=\"moneyPlayer\" placeholder=\"Ví dụ: admin1\">" +
                "        </div>" +
                "        <div class=\"form-group\">" +
                "            <label>Loại tiền:</label>" +
                "            <select id=\"moneyType\" style=\"width:100%; padding:10px; border-radius:6px; border:1px solid #bdc3c7; font-size:14px; box-sizing: border-box;\">" +
                "                <option value=\"1\">Vàng</option>" +
                "                <option value=\"2\">Ngọc Xanh</option>" +
                "                <option value=\"3\">Hồng Ngọc</option>" +
                "            </select>" +
                "        </div>" +
                "        <div class=\"form-group\">" +
                "            <label>Số lượng:</label>" +
                "            <input type=\"number\" id=\"moneyAmount\" placeholder=\"Nhập số lượng\">" +
                "        </div>" +
                "        <button onclick=\"addMoney()\">Cộng Tiền</button>" +
                "        <div id=\"resultMoney\" class=\"result\"></div>" +
                "    </div>" +
                "    <script>" +
                "        let allItems = [];" +
                "        fetch('/api/items').then(r => r.json()).then(data => { allItems = data; });" +
                "        function filterItems() {" +
                "            const q = document.getElementById('searchBox').value.toLowerCase();" +
                "            const list = document.getElementById('itemList');" +
                "            if(!q) { list.style.display = 'none'; return; }" +
                "            list.style.display = 'block'; list.innerHTML = '';" +
                "            const filtered = allItems.filter(i => i.name.toLowerCase().includes(q)).slice(0, 30);" +
                "            filtered.forEach(i => {" +
                "                const div = document.createElement('div');" +
                "                div.className = 'item-entry';" +
                "                div.innerText = '[' + i.id + '] ' + i.name;" +
                "                div.onclick = () => {" +
                "                    document.getElementById('itemId').value = i.id;" +
                "                    document.getElementById('searchBox').value = i.name;" +
                "                    list.style.display = 'none';" +
                "                };" +
                "                list.appendChild(div);" +
                "            });" +
                "        }" +
                "        function showRes(elId, text, isSuccess) {" +
                "            const div = document.getElementById(elId);" +
                "            div.className = 'result ' + (isSuccess ? 'success' : 'error');" +
                "            div.innerText = text;" +
                "        }" +
                "        function addItem() {" +
                "            const player = document.getElementById('player').value;" +
                "            const itemId = document.getElementById('itemId').value;" +
                "            const quantity = document.getElementById('quantity').value;" +
                "            const options = document.getElementById('options').value;" +
                "            if(!player || !itemId) return showRes('resultItem', 'Vui lòng nhập đủ thông tin!', false);" +
                "            fetch('/api/add-item?player=' + encodeURIComponent(player) + '&itemId=' + itemId + '&quantity=' + quantity + '&options=' + encodeURIComponent(options))" +
                "            .then(r => r.text())" +
                "            .then(text => showRes('resultItem', text, text.includes('thành công')))" +
                "            .catch(e => showRes('resultItem', 'Lỗi kết nối!', false));" +
                "        }" +
                "        function register() {" +
                "            const user = document.getElementById('regUser').value;" +
                "            const pass = document.getElementById('regPass').value;" +
                "            if(!user || !pass) return showRes('resultReg', 'Vui lòng nhập đủ thông tin!', false);" +
                "            fetch('/api/register?username=' + encodeURIComponent(user) + '&password=' + encodeURIComponent(pass))" +
                "            .then(r => r.text())" +
                "            .then(text => showRes('resultReg', text, text.includes('thành công')))" +
                "            .catch(e => showRes('resultReg', 'Lỗi kết nối!', false));" +
                "        }" +
                "        function addMoney() {" +
                "            const player = document.getElementById('moneyPlayer').value;" +
                "            const type = document.getElementById('moneyType').value;" +
                "            const amount = document.getElementById('moneyAmount').value;" +
                "            if(!player || !amount) return showRes('resultMoney', 'Vui lòng nhập đủ thông tin!', false);" +
                "            fetch('/api/add-money?player=' + encodeURIComponent(player) + '&type=' + type + '&amount=' + amount)" +
                "            .then(r => r.text())" +
                "            .then(text => showRes('resultMoney', text, text.includes('thành công')))" +
                "            .catch(e => showRes('resultMoney', 'Lỗi kết nối!', false));" +
                "        }" +
                "    </script>" +
                "</body>" +
                "</html>";
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, html.getBytes("UTF-8").length);
            OutputStream os = exchange.getResponseBody();
            os.write(html.getBytes("UTF-8"));
            os.close();
        }
    }

    static class AddItemHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "";
            try {
                URI uri = exchange.getRequestURI();
                Map<String, String> query = parseQuery(uri.getQuery());
                String playerName = query.get("player");
                int itemId = Integer.parseInt(query.get("itemId"));
                int quantity = Integer.parseInt(query.get("quantity"));
                String optionsStr = query.get("options");

                Player player = Client.gI().getPlayer(playerName);
                if (player == null) {
                    response = "Lỗi: Nhân vật '" + playerName + "' không online hoặc không tồn tại!";
                } else {
                    Item item = ItemService.gI().createNewItem((short) itemId, quantity);
                    if (item == null || item.template == null) {
                        response = "Lỗi: ID vật phẩm không hợp lệ!";
                    } else {
                        if (optionsStr != null && !optionsStr.trim().isEmpty()) {
                            try {
                                String[] opts = optionsStr.split(";");
                                for (String opt : opts) {
                                    if(opt.trim().isEmpty()) continue;
                                    String[] parts = opt.split(":");
                                    if (parts.length == 2) {
                                        int optId = Integer.parseInt(parts[0].trim());
                                        int param = Integer.parseInt(parts[1].trim());
                                        item.itemOptions.add(new nro.models.item.Item.ItemOption(optId, param));
                                    }
                                }
                            } catch (Exception ex) {
                                // Ignore parse errors
                            }
                        }
                        InventoryService.gI().addItemBag(player, item);
                        InventoryService.gI().sendItemBags(player);
                        Service.gI().sendThongBao(player, "Bạn nhận được " + quantity + " " + item.template.name + " từ Admin Panel");
                        response = "Thêm thành công " + quantity + " " + item.template.name + " cho " + playerName + "!";
                    }
                }
            } catch (Exception e) {
                response = "Lỗi: " + e.getMessage();
            }
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes("UTF-8"));
            os.close();
        }

        private Map<String, String> parseQuery(String query) {
            Map<String, String> result = new HashMap<>();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] entry = param.split("=");
                    if (entry.length > 1) {
                        result.put(entry[0], java.net.URLDecoder.decode(entry[1], java.nio.charset.StandardCharsets.UTF_8));
                    } else {
                        result.put(entry[0], "");
                    }
                }
            }
            return result;
        }
    }

    static class AddMoneyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "";
            try {
                URI uri = exchange.getRequestURI();
                Map<String, String> query = parseQuery(uri.getQuery());
                String playerName = query.get("player");
                int type = Integer.parseInt(query.get("type"));
                long amount = Long.parseLong(query.get("amount"));

                Player player = Client.gI().getPlayer(playerName);
                if (player == null) {
                    response = "Lỗi: Nhân vật '" + playerName + "' không online hoặc không tồn tại!";
                } else {
                    if (type == 1) {
                        player.inventory.gold += amount;
                        if (player.inventory.gold > 2000000000) player.inventory.gold = 2000000000;
                    } else if (type == 2) {
                        player.inventory.gem += amount;
                    } else if (type == 3) {
                        player.inventory.ruby += amount;
                    }
                    Service.gI().sendMoney(player);
                    Service.gI().sendThongBao(player, "Bạn nhận được tiền từ Admin Panel");
                    response = "Cộng thành công " + amount + " tiền cho " + playerName + "!";
                }
            } catch (Exception e) {
                response = "Lỗi: " + e.getMessage();
            }
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes("UTF-8"));
            os.close();
        }

        private Map<String, String> parseQuery(String query) {
            Map<String, String> result = new HashMap<>();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] entry = param.split("=");
                    if (entry.length > 1) {
                        result.put(entry[0], java.net.URLDecoder.decode(entry[1], java.nio.charset.StandardCharsets.UTF_8));
                    } else {
                        result.put(entry[0], "");
                    }
                }
            }
            return result;
        }
    }

    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "";
            try {
                URI uri = exchange.getRequestURI();
                Map<String, String> query = parseQuery(uri.getQuery());
                String user = query.get("username");
                String pass = query.get("password");

                if (user == null || pass == null || user.trim().isEmpty() || pass.trim().isEmpty()) {
                    response = "Lỗi: Tài khoản và mật khẩu không được để trống!";
                } else {
                    try (java.sql.Connection conn = nro.models.data.LocalManager.gI().getConnection()) {
                        java.sql.PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM account WHERE username = ?");
                        ps.setString(1, user);
                        java.sql.ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            response = "Lỗi: Tài khoản đã tồn tại!";
                        } else {
                            java.sql.PreparedStatement insert = conn.prepareStatement("INSERT INTO account (username, password, email, token, xsrf_token, newpass) VALUES (?, ?, '', '', '', '')");
                            insert.setString(1, user);
                            insert.setString(2, pass);
                            insert.executeUpdate();
                            response = "Đăng ký thành công tài khoản: " + user;
                        }
                    }
                }
            } catch (Exception e) {
                response = "Lỗi Database: " + e.getMessage();
            }
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes("UTF-8"));
            os.close();
        }

        private Map<String, String> parseQuery(String query) {
            Map<String, String> result = new HashMap<>();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] entry = param.split("=");
                    if (entry.length > 1) {
                        result.put(entry[0], java.net.URLDecoder.decode(entry[1], java.nio.charset.StandardCharsets.UTF_8));
                    } else {
                        result.put(entry[0], "");
                    }
                }
            }
            return result;
        }
    }
    static class ItemsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (nro.models.player_system.Template.ItemTemplate item : nro.models.server.Manager.ITEM_TEMPLATES) {
                if (item != null && item.name != null) {
                    if (!first) sb.append(",");
                    sb.append("{\"id\":").append(item.id).append(",\"name\":\"")
                      .append(item.name.replace("\\", "\\\\").replace("\"", "\\\"")).append("\"}");
                    first = false;
                }
            }
            sb.append("]");
            byte[] bytes = sb.toString().getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
}
