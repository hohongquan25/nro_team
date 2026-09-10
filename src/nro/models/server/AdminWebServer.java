package nro.models.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;

public class AdminWebServer {
    private HttpServer server;
    private static final String LOGIN_NOTICE_FILE = "data/login_notice.txt";
    private static String loginNotice = "";

    public static String getLoginNotice() {
        if (loginNotice == null || loginNotice.isEmpty()) {
            loadLoginNotice();
        }
        return loginNotice;
    }

    public static void loadLoginNotice() {
        try {
            File file = new File(LOGIN_NOTICE_FILE);
            if (file.exists()) {
                loginNotice = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            } else {
                loginNotice = "Chào mừng gay đến với ROC.\n"
                        + "Sever free mọi thứ.\n"
                        + "Chi tiết xem tại diễn đàn.";
                saveLoginNotice(loginNotice);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveLoginNotice(String text) {
        try {
            loginNotice = text;
            File file = new File(LOGIN_NOTICE_FILE);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            loadLoginNotice();
            server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/", new RootHandler());
            server.createContext("/admin", new UIHandler());
            server.createContext("/api/notifies", new NotifiesHandler());
            server.createContext("/api/add-notify", new AddNotifyHandler());
            server.createContext("/api/update-notify", new UpdateNotifyHandler());
            server.createContext("/api/delete-notify", new DeleteNotifyHandler());
            server.createContext("/api/broadcast", new BroadcastHandler());
            server.createContext("/api/login-notice", new LoginNoticeHandler());
            server.createContext("/api/add-item", new AddItemHandler());
            server.createContext("/api/add-money", new AddMoneyHandler());
            server.createContext("/api/register", new RegisterHandler());
            server.createContext("/api/items", new ItemsHandler());
            server.setExecutor(null);
            server.start();
            System.out
                    .println("Admin Web Server started on port 8080. Truy cap http://localhost:8080/admin de su dung.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void reloadNotify() {
        try (Connection conn = LocalManager.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT name, text FROM notify ORDER BY id DESC");
                ResultSet rs = ps.executeQuery()) {
            List<String> list = new ArrayList<>();
            while (rs.next()) {
                list.add(rs.getString("name") + "<>" + rs.getString("text"));
            }
            synchronized (Manager.NOTIFY) {
                Manager.NOTIFY.clear();
                Manager.NOTIFY.addAll(list);
            }
            for (Player p : Client.gI().getPlayers()) {
                if (p != null && p.session != null) {
                    ServerNotify.gI().sendNotifyTab(p);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendResponse(HttpExchange exchange, String response, String contentType, int code)
            throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static Map<String, String> parseParams(HttpExchange exchange) throws IOException {
        Map<String, String> result = new HashMap<>();
        URI uri = exchange.getRequestURI();
        if (uri.getQuery() != null) {
            parseQueryString(uri.getQuery(), result);
        }
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try (InputStream is = exchange.getRequestBody();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[1024];
                int n;
                while ((n = is.read(buf)) != -1) {
                    bos.write(buf, 0, n);
                }
                String body = bos.toString(StandardCharsets.UTF_8);
                parseQueryString(body, result);
            }
        }
        return result;
    }

    private static void parseQueryString(String query, Map<String, String> result) {
        if (query != null && !query.isEmpty()) {
            for (String param : query.split("&")) {
                String[] entry = param.split("=", 2);
                if (entry.length > 0) {
                    String key = entry[0];
                    String val = entry.length > 1 ? URLDecoder.decode(entry[1], StandardCharsets.UTF_8) : "";
                    result.put(key, val);
                }
            }
        }
    }

    private static String escapeJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Location", "/admin");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        }
    }

    static class UIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = "<!DOCTYPE html>\n" +
                    "<html lang=\"vi\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                    "    <title>NRO Admin Control Panel</title>\n" +
                    "    <link href=\"https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap\" rel=\"stylesheet\">\n"
                    +
                    "    <style>\n" +
                    "        :root {\n" +
                    "            --bg: #0f172a;\n" +
                    "            --surface: #1e293b;\n" +
                    "            --surface-hover: #334155;\n" +
                    "            --primary: #3b82f6;\n" +
                    "            --primary-glow: rgba(59, 130, 246, 0.4);\n" +
                    "            --accent: #10b981;\n" +
                    "            --danger: #ef4444;\n" +
                    "            --warning: #f59e0b;\n" +
                    "            --text: #f8fafc;\n" +
                    "            --text-muted: #94a3b8;\n" +
                    "            --border: #334155;\n" +
                    "            --radius: 12px;\n" +
                    "        }\n" +
                    "        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Outfit', sans-serif; }\n"
                    +
                    "        body { background: var(--bg); color: var(--text); min-height: 100vh; padding: 24px; }\n" +
                    "        .navbar { max-width: 1200px; margin: 0 auto 28px; display: flex; align-items: center; justify-content: space-between; padding: 18px 28px; background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius); box-shadow: 0 10px 30px rgba(0,0,0,0.3); }\n"
                    +
                    "        .brand { font-size: 22px; font-weight: 700; background: linear-gradient(135deg, #60a5fa, #a855f7); -webkit-background-clip: text; -webkit-text-fill-color: transparent; display: flex; align-items: center; gap: 10px; }\n"
                    +
                    "        .nav-tabs { display: flex; gap: 8px; flex-wrap: wrap; }\n" +
                    "        .nav-btn { background: transparent; border: 1px solid transparent; color: var(--text-muted); padding: 10px 18px; border-radius: 8px; cursor: pointer; font-size: 15px; font-weight: 500; transition: all 0.25s ease; }\n"
                    +
                    "        .nav-btn:hover { color: var(--text); background: var(--surface-hover); }\n" +
                    "        .nav-btn.active { background: var(--primary); color: white; border-color: var(--primary); box-shadow: 0 0 16px var(--primary-glow); }\n"
                    +
                    "        .main-container { max-width: 1200px; margin: 0 auto; }\n" +
                    "        .tab-content { display: none; }\n" +
                    "        .tab-content.active { display: block; animation: fadeIn 0.3s ease; }\n" +
                    "        @keyframes fadeIn { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }\n"
                    +
                    "        .card { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius); padding: 26px; margin-bottom: 24px; box-shadow: 0 8px 24px rgba(0,0,0,0.25); }\n"
                    +
                    "        .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }\n"
                    +
                    "        .card-title { font-size: 18px; font-weight: 600; color: #e2e8f0; display: flex; align-items: center; gap: 8px; }\n"
                    +
                    "        .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px; margin-bottom: 16px; }\n"
                    +
                    "        .form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 16px; }\n" +
                    "        label { font-size: 14px; font-weight: 500; color: var(--text-muted); }\n" +
                    "        input, select, textarea { background: #0f172a; border: 1px solid var(--border); border-radius: 8px; padding: 12px 14px; color: var(--text); font-size: 14px; outline: none; transition: border 0.2s; }\n"
                    +
                    "        input:focus, select:focus, textarea:focus { border-color: var(--primary); }\n" +
                    "        textarea { min-height: 110px; resize: vertical; }\n" +
                    "        .btn { display: inline-flex; align-items: center; justify-content: center; gap: 8px; padding: 12px 22px; border-radius: 8px; border: none; font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.2s; text-decoration: none; }\n"
                    +
                    "        .btn-primary { background: var(--primary); color: white; }\n" +
                    "        .btn-primary:hover { background: #2563eb; transform: translateY(-1px); }\n" +
                    "        .btn-accent { background: var(--accent); color: white; }\n" +
                    "        .btn-accent:hover { background: #059669; }\n" +
                    "        .btn-danger { background: var(--danger); color: white; }\n" +
                    "        .btn-danger:hover { background: #dc2626; }\n" +
                    "        .btn-sm { padding: 6px 12px; font-size: 13px; border-radius: 6px; }\n" +
                    "        .btn-refresh { background: var(--surface-hover); color: var(--text); border: 1px solid var(--border); }\n"
                    +
                    "        .btn-refresh:hover { background: #475569; }\n" +
                    "        table { width: 100%; border-collapse: collapse; margin-top: 10px; }\n" +
                    "        th, td { padding: 14px 16px; text-align: left; border-bottom: 1px solid var(--border); font-size: 14px; }\n"
                    +
                    "        th { background: #172033; color: var(--text-muted); font-weight: 600; text-transform: uppercase; font-size: 12px; letter-spacing: 0.5px; }\n"
                    +
                    "        tr:hover td { background: rgba(255,255,256,0.02); }\n" +
                    "        .item-list { max-height: 160px; overflow-y: auto; border: 1px solid var(--border); border-radius: 8px; margin-top: 4px; display: none; background: #0f172a; position: absolute; width: calc(100% - 28px); z-index: 10; }\n"
                    +
                    "        .item-entry { padding: 9px 12px; cursor: pointer; border-bottom: 1px solid #1e293b; font-size: 13px; }\n"
                    +
                    "        .item-entry:hover { background: var(--surface-hover); color: var(--primary); }\n" +
                    "        .badge { display: inline-block; padding: 4px 8px; border-radius: 6px; font-size: 12px; font-weight: 600; background: #334155; color: #cbd5e1; }\n"
                    +
                    "        .toast { position: fixed; bottom: 24px; right: 24px; padding: 14px 22px; border-radius: 10px; color: white; font-weight: 500; display: none; box-shadow: 0 10px 30px rgba(0,0,0,0.4); z-index: 100; animation: toastIn 0.3s ease; }\n"
                    +
                    "        @keyframes toastIn { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }\n"
                    +
                    "        .toast.success { background: var(--accent); display: block; }\n" +
                    "        .toast.error { background: var(--danger); display: block; }\n" +
                    "        .modal { display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.65); backdrop-filter: blur(4px); align-items: center; justify-content: center; z-index: 99; }\n"
                    +
                    "        .modal.active { display: flex; }\n" +
                    "        .modal-box { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius); width: 550px; max-width: 90%; padding: 26px; box-shadow: 0 20px 50px rgba(0,0,0,0.5); }\n"
                    +
                    "        .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }\n"
                    +
                    "        .modal-title { font-size: 18px; font-weight: 600; }\n" +
                    "        .btn-close { background: none; border: none; font-size: 22px; color: var(--text-muted); cursor: pointer; }\n"
                    +
                    "        .btn-close:hover { color: var(--text); }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class=\"navbar\">\n" +
                    "        <div class=\"brand\">🐲 NRO ADMIN PORTAL</div>\n" +
                    "        <div class=\"nav-tabs\">\n" +
                    "            <button class=\"nav-btn active\" onclick=\"switchTab('tab-notify')\">📢 Bảng Thông Báo</button>\n"
                    +
                    "            <button class=\"nav-btn\" onclick=\"switchTab('tab-broadcast')\">⚡ Chạy Chữ Server</button>\n"
                    +
                    "            <button class=\"nav-btn\" onclick=\"switchTab('tab-login-notice')\">👋 Thông Báo Vào Game</button>\n"
                    +
                    "            <button class=\"nav-btn\" onclick=\"switchTab('tab-item')\">🎁 Thêm Vật Phẩm</button>\n"
                    +
                    "            <button class=\"nav-btn\" onclick=\"switchTab('tab-money')\">💰 Cộng Tiền</button>\n" +
                    "            <button class=\"nav-btn\" onclick=\"switchTab('tab-reg')\">👤 Đăng Ký TK</button>\n" +
                    "        </div>\n" +
                    "    </div>\n" +
                    "\n" +
                    "    <div class=\"main-container\">\n" +
                    "        <!-- TAB 1: BẢNG THÔNG BÁO -->\n" +
                    "        <div id=\"tab-notify\" class=\"tab-content active\">\n" +
                    "            <div class=\"card\">\n" +
                    "                <div class=\"card-title\">➕ Thêm Thông Báo Mới (Hiển thị trong game tab Thông Báo)</div>\n"
                    +
                    "                <div style=\"margin-top: 16px;\">\n" +
                    "                    <div class=\"form-group\">\n" +
                    "                        <label>Tiêu đề thông báo:</label>\n" +
                    "                        <input type=\"text\" id=\"addNotifyName\" placeholder=\"Ví dụ: Sự kiện Đua Top, Lịch bảo trì, Khuyến mãi nạp...\">\n"
                    +
                    "                    </div>\n" +
                    "                    <div class=\"form-group\">\n" +
                    "                        <label>Nội dung chi tiết (hỗ trợ nhiều dòng):</label>\n" +
                    "                        <textarea id=\"addNotifyText\" placeholder=\"Nhập nội dung chi tiết thông báo cho toàn bộ cư dân...\"></textarea>\n"
                    +
                    "                    </div>\n" +
                    "                    <button class=\"btn btn-primary\" onclick=\"createNotify()\">Đăng Thông Báo Ngay</button>\n"
                    +
                    "                </div>\n" +
                    "            </div>\n" +
                    "\n" +
                    "            <div class=\"card\">\n" +
                    "                <div class=\"card-header\">\n" +
                    "                    <div class=\"card-title\">📋 Danh Sách Thông Báo Đang Hoạt Động</div>\n" +
                    "                    <button class=\"btn btn-refresh btn-sm\" onclick=\"loadNotifies()\">🔄 Làm Mới</button>\n"
                    +
                    "                </div>\n" +
                    "                <div style=\"overflow-x: auto;\">\n" +
                    "                    <table>\n" +
                    "                        <thead>\n" +
                    "                            <tr>\n" +
                    "                                <th style=\"width: 80px;\">ID</th>\n" +
                    "                                <th style=\"width: 250px;\">Tiêu Đề</th>\n" +
                    "                                <th>Nội Dung</th>\n" +
                    "                                <th style=\"width: 150px; text-align: center;\">Thao Tác</th>\n" +
                    "                            </tr>\n" +
                    "                        </thead>\n" +
                    "                        <tbody id=\"notifyTableBody\">\n" +
                    "                            <tr><td colspan=\"4\" style=\"text-align: center; color: var(--text-muted);\">Đang tải dữ liệu...</td></tr>\n"
                    +
                    "                        </tbody>\n" +
                    "                    </table>\n" +
                    "                </div>\n" +
                    "            </div>\n" +
                    "        </div>\n" +
                    "\n" +
                    "        <!-- TAB 2: PHÁT CHẠY CHỮ SERVER -->\n" +
                    "        <div id=\"tab-broadcast\" class=\"tab-content\">\n" +
                    "            <div class=\"card\">\n" +
                    "                <div class=\"card-title\">⚡ Phát Thông Báo Chạy Chữ Trực Tiếp (Message 93 toàn server)</div>\n"
                    +
                    "                <p style=\"color: var(--text-muted); margin: 8px 0 20px; font-size: 14px;\">Dòng thông báo này sẽ lập tức chạy ngang màn hình của tất cả người chơi đang đăng nhập mà không cần khởi động lại server.</p>\n"
                    +
                    "                <div class=\"form-group\">\n" +
                    "                    <label>Nội dung cần phát loa:</label>\n" +
                    "                    <input type=\"text\" id=\"broadcastMsg\" placeholder=\"Ví dụ: Server sẽ bảo trì trong 5 phút nữa, các cư dân vui lòng lưu ý!\">\n"
                    +
                    "                </div>\n" +
                    "                <button class=\"btn btn-accent\" onclick=\"sendBroadcast()\">📢 Phát Thông Báo Toàn Server</button>\n"
                    +
                    "            </div>\n" +
                    "        </div>\n" +
                    "\n" +
                    "        <!-- TAB 6: THÔNG BÁO POP-UP KHI VÀO GAME -->\n" +
                    "        <div id=\"tab-login-notice\" class=\"tab-content\">\n" +
                    "            <div class=\"card\">\n" +
                    "                <div class=\"card-header\">\n" +
                    "                    <div class=\"card-title\">👋 Bảng Pop-up Admin Khi Đăng Nhập (Big Message)</div>\n"
                    +
                    "                    <button class=\"btn btn-refresh btn-sm\" onclick=\"loadLoginNotice()\">🔄 Làm Mới</button>\n"
                    +
                    "                </div>\n" +
                    "                <p style=\"color: var(--text-muted); margin: 8px 0 20px; font-size: 14px; line-height: 1.6;\">\n"
                    +
                    "                    Bảng thông báo này xuất hiện ở giữa màn hình (kèm hình đại diện Admin Quy Lão mũ tím) khi người chơi <b>vừa đăng nhập vào game</b>.<br>\n"
                    +
                    "                    Bạn có thể chỉnh sửa nội dung bên dưới và bấm Lưu để cập nhật ngay lập tức mà <b>không cần tắt/bật lại server</b>!\n"
                    +
                    "                </p>\n" +
                    "                <div class=\"form-group\">\n" +
                    "                    <label>Nội dung thông báo (hỗ trợ xuống dòng):</label>\n" +
                    "                    <textarea id=\"loginNoticeContent\" rows=\"6\" style=\"min-height: 150px; font-size: 15px; line-height: 1.6;\"></textarea>\n"
                    +
                    "                </div>\n" +
                    "                <div style=\"display: flex; gap: 12px; flex-wrap: wrap; margin-top: 10px;\">\n" +
                    "                    <button class=\"btn btn-primary\" onclick=\"saveLoginNotice(false)\">💾 Lưu Thông Báo</button>\n"
                    +
                    "                    <button class=\"btn btn-accent\" onclick=\"saveLoginNotice(true)\">⚡ Lưu & Gửi Ngay Cho Tất Cả Người Chơi Online</button>\n"
                    +
                    "                    <button class=\"btn btn-danger\" onclick=\"clearLoginNotice()\">❌ Xóa / Tắt Bảng Pop-up</button>\n"
                    +
                    "                </div>\n" +
                    "            </div>\n" +
                    "        </div>\n" +
                    "\n" +
                    "        <!-- TAB 3: THÊM VẬT PHẨM -->\n" +
                    "        <div id=\"tab-item\" class=\"tab-content\">\n" +
                    "            <div class=\"card\" style=\"max-width: 600px; margin: 0 auto;\">\n" +
                    "                <div class=\"card-title\">🎁 Thêm Vật Phẩm Vào Hành Trang</div>\n" +
                    "                <div style=\"margin-top: 16px;\">\n" +
                    "                    <div class=\"form-group\">\n" +
                    "                        <label>Tên nhân vật (đang online):</label>\n" +
                    "                        <input type=\"text\" id=\"player\" placeholder=\"Ví dụ: admin1\">\n" +
                    "                    </div>\n" +
                    "                    <div class=\"form-group\" style=\"position: relative;\">\n" +
                    "                        <label>Tìm vật phẩm:</label>\n" +
                    "                        <input type=\"text\" id=\"searchBox\" onkeyup=\"filterItems()\" placeholder=\"Gõ tên để tìm kiếm nhanh...\">\n"
                    +
                    "                        <div id=\"itemList\" class=\"item-list\"></div>\n" +
                    "                    </div>\n" +
                    "                    <div class=\"form-grid\">\n" +
                    "                        <div class=\"form-group\">\n" +
                    "                            <label>ID Vật phẩm:</label>\n" +
                    "                            <input type=\"number\" id=\"itemId\" placeholder=\"Ví dụ: 457\">\n" +
                    "                        </div>\n" +
                    "                        <div class=\"form-group\">\n" +
                    "                            <label>Số lượng:</label>\n" +
                    "                            <input type=\"number\" id=\"quantity\" value=\"1\">\n" +
                    "                        </div>\n" +
                    "                    </div>\n" +
                    "                    <button class=\"btn btn-primary\" style=\"width: 100%;\" onclick=\"addItem()\">Gửi Vật Phẩm</button>\n"
                    +
                    "                </div>\n" +
                    "            </div>\n" +
                    "        </div>\n" +
                    "\n" +
                    "        <!-- TAB 4: CỘNG TIỀN -->\n" +
                    "        <div id=\"tab-money\" class=\"tab-content\">\n" +
                    "            <div class=\"card\" style=\"max-width: 550px; margin: 0 auto;\">\n" +
                    "                <div class=\"card-title\">💰 Cộng Tiền Cho Nhân Vật</div>\n" +
                    "                <div style=\"margin-top: 16px;\">\n" +
                    "                    <div class=\"form-group\">\n" +
                    "                        <label>Tên nhân vật (đang online):</label>\n" +
                    "                        <input type=\"text\" id=\"moneyPlayer\" placeholder=\"Ví dụ: admin1\">\n" +
                    "                    </div>\n" +
                    "                    <div class=\"form-group\">\n" +
                    "                        <label>Loại tiền:</label>\n" +
                    "                        <select id=\"moneyType\">\n" +
                    "                            <option value=\"1\">Vàng (Gold)</option>\n" +
                    "                            <option value=\"2\">Ngọc Xanh (Gem)</option>\n" +
                    "                            <option value=\"3\">Hồng Ngọc (Ruby)</option>\n" +
                    "                        </select>\n" +
                    "                    </div>\n" +
                    "                    <div class=\"form-group\">\n" +
                    "                        <label>Số lượng cần cộng:</label>\n" +
                    "                        <input type=\"number\" id=\"moneyAmount\" placeholder=\"Nhập số lượng\">\n"
                    +
                    "                    </div>\n" +
                    "                    <button class=\"btn btn-primary\" style=\"width: 100%;\" onclick=\"addMoney()\">Cộng Tiền Ngay</button>\n"
                    +
                    "                </div>\n" +
                    "            </div>\n" +
                    "        </div>\n" +
                    "\n" +
                    "        <!-- TAB 5: ĐĂNG KÝ TÀI KHOẢN -->\n" +
                    "        <div id=\"tab-reg\" class=\"tab-content\">\n" +
                    "            <div class=\"card\" style=\"max-width: 500px; margin: 0 auto;\">\n" +
                    "                <div class=\"card-title\">👤 Đăng Ký Tài Khoản Mới</div>\n" +
                    "                <div style=\"margin-top: 16px;\">\n" +
                    "                    <div class=\"form-group\">\n" +
                    "                        <label>Tài khoản đăng nhập:</label>\n" +
                    "                        <input type=\"text\" id=\"regUser\" placeholder=\"Nhập tên tài khoản\">\n"
                    +
                    "                    </div>\n" +
                    "                    <div class=\"form-group\">\n" +
                    "                        <label>Mật khẩu:</label>\n" +
                    "                        <input type=\"password\" id=\"regPass\" placeholder=\"Nhập mật khẩu\">\n" +
                    "                    </div>\n" +
                    "                    <button class=\"btn btn-accent\" style=\"width: 100%;\" onclick=\"register()\">Tạo Tài Khoản</button>\n"
                    +
                    "                </div>\n" +
                    "            </div>\n" +
                    "        </div>\n" +
                    "    </div>\n" +
                    "\n" +
                    "    <!-- MODAL SỬA THÔNG BÁO -->\n" +
                    "    <div id=\"editModal\" class=\"modal\">\n" +
                    "        <div class=\"modal-box\">\n" +
                    "            <div class=\"modal-header\">\n" +
                    "                <div class=\"modal-title\">✏️ Chỉnh Sửa Thông Báo</div>\n" +
                    "                <button class=\"btn-close\" onclick=\"closeEditModal()\">&times;</button>\n" +
                    "            </div>\n" +
                    "            <input type=\"hidden\" id=\"editNotifyId\">\n" +
                    "            <div class=\"form-group\">\n" +
                    "                <label>Tiêu đề thông báo:</label>\n" +
                    "                <input type=\"text\" id=\"editNotifyName\">\n" +
                    "            </div>\n" +
                    "            <div class=\"form-group\">\n" +
                    "                <label>Nội dung chi tiết:</label>\n" +
                    "                <textarea id=\"editNotifyText\"></textarea>\n" +
                    "            </div>\n" +
                    "            <div style=\"display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px;\">\n"
                    +
                    "                <button class=\"btn btn-refresh\" onclick=\"closeEditModal()\">Hủy</button>\n" +
                    "                <button class=\"btn btn-primary\" onclick=\"saveEditNotify()\">Lưu Thay Đổi</button>\n"
                    +
                    "            </div>\n" +
                    "        </div>\n" +
                    "    </div>\n" +
                    "\n" +
                    "    <div id=\"toast\" class=\"toast\"></div>\n" +
                    "\n" +
                    "    <script>\n" +
                    "        function switchTab(tabId) {\n" +
                    "            document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));\n"
                    +
                    "            document.querySelectorAll('.nav-btn').forEach(el => el.classList.remove('active'));\n"
                    +
                    "            document.getElementById(tabId).classList.add('active');\n" +
                    "            event.target.classList.add('active');\n" +
                    "            if (tabId === 'tab-notify') loadNotifies();\n" +
                    "        }\n" +
                    "\n" +
                    "        function showToast(msg, isSuccess = true) {\n" +
                    "            const t = document.getElementById('toast');\n" +
                    "            t.className = 'toast ' + (isSuccess ? 'success' : 'error');\n" +
                    "            t.innerText = msg;\n" +
                    "            setTimeout(() => { t.className = 'toast'; }, 3500);\n" +
                    "        }\n" +
                    "\n" +
                    "        let notifiesData = [];\n" +
                    "        function loadNotifies() {\n" +
                    "            fetch('/api/notifies')\n" +
                    "                .then(r => r.json())\n" +
                    "                .then(data => {\n" +
                    "                    notifiesData = data;\n" +
                    "                    const tbody = document.getElementById('notifyTableBody');\n" +
                    "                    if (data.length === 0) {\n" +
                    "                        tbody.innerHTML = '<tr><td colspan=\"4\" style=\"text-align: center; color: var(--text-muted);\">Chưa có thông báo nào trong database.</td></tr>';\n"
                    +
                    "                        return;\n" +
                    "                    }\n" +
                    "                    let html = '';\n" +
                    "                    data.forEach(item => {\n" +
                    "                        html += `<tr>\n" +
                    "                            <td><span class=\"badge\">#${item.id}</span></td>\n" +
                    "                            <td style=\"font-weight: 600; color: #60a5fa;\">${escapeHtml(item.name)}</td>\n"
                    +
                    "                            <td style=\"white-space: pre-line; color: #cbd5e1;\">${escapeHtml(item.text)}</td>\n"
                    +
                    "                            <td style=\"text-align: center;\">\n" +
                    "                                <button class=\"btn btn-refresh btn-sm\" onclick=\"openEditModal(${item.id})\">✏️ Sửa</button>\n"
                    +
                    "                                <button class=\"btn btn-danger btn-sm\" onclick=\"deleteNotify(${item.id})\">🗑️ Xóa</button>\n"
                    +
                    "                            </td>\n" +
                    "                        </tr>`;\n" +
                    "                    });\n" +
                    "                    tbody.innerHTML = html;\n" +
                    "                })\n" +
                    "                .catch(e => showToast('Lỗi nạp danh sách thông báo!', false));\n" +
                    "        }\n" +
                    "\n" +
                    "        function createNotify() {\n" +
                    "            const name = document.getElementById('addNotifyName').value.trim();\n" +
                    "            const text = document.getElementById('addNotifyText').value.trim();\n" +
                    "            if (!name || !text) return showToast('Vui lòng nhập đầy đủ tiêu đề và nội dung!', false);\n"
                    +
                    "            const formData = new URLSearchParams();\n" +
                    "            formData.append('name', name);\n" +
                    "            formData.append('text', text);\n" +
                    "            fetch('/api/add-notify', { method: 'POST', body: formData })\n" +
                    "                .then(r => r.text())\n" +
                    "                .then(msg => {\n" +
                    "                    showToast(msg, msg.includes('thành công'));\n" +
                    "                    document.getElementById('addNotifyName').value = '';\n" +
                    "                    document.getElementById('addNotifyText').value = '';\n" +
                    "                    loadNotifies();\n" +
                    "                });\n" +
                    "        }\n" +
                    "\n" +
                    "        function openEditModal(id) {\n" +
                    "            const item = notifiesData.find(x => x.id === id);\n" +
                    "            if (!item) return;\n" +
                    "            document.getElementById('editNotifyId').value = item.id;\n" +
                    "            document.getElementById('editNotifyName').value = item.name;\n" +
                    "            document.getElementById('editNotifyText').value = item.text;\n" +
                    "            document.getElementById('editModal').classList.add('active');\n" +
                    "        }\n" +
                    "\n" +
                    "        function closeEditModal() {\n" +
                    "            document.getElementById('editModal').classList.remove('active');\n" +
                    "        }\n" +
                    "\n" +
                    "        function saveEditNotify() {\n" +
                    "            const id = document.getElementById('editNotifyId').value;\n" +
                    "            const name = document.getElementById('editNotifyName').value.trim();\n" +
                    "            const text = document.getElementById('editNotifyText').value.trim();\n" +
                    "            if (!name || !text) return showToast('Vui lòng nhập đủ thông tin!', false);\n" +
                    "            const formData = new URLSearchParams();\n" +
                    "            formData.append('id', id);\n" +
                    "            formData.append('name', name);\n" +
                    "            formData.append('text', text);\n" +
                    "            fetch('/api/update-notify', { method: 'POST', body: formData })\n" +
                    "                .then(r => r.text())\n" +
                    "                .then(msg => {\n" +
                    "                    showToast(msg, msg.includes('thành công'));\n" +
                    "                    closeEditModal();\n" +
                    "                    loadNotifies();\n" +
                    "                });\n" +
                    "        }\n" +
                    "\n" +
                    "        function deleteNotify(id) {\n" +
                    "            if (!confirm('Bạn có chắc chắn muốn xóa thông báo #' + id + ' không?')) return;\n" +
                    "            const formData = new URLSearchParams();\n" +
                    "            formData.append('id', id);\n" +
                    "            fetch('/api/delete-notify', { method: 'POST', body: formData })\n" +
                    "                .then(r => r.text())\n" +
                    "                .then(msg => {\n" +
                    "                    showToast(msg, msg.includes('thành công'));\n" +
                    "                    loadNotifies();\n" +
                    "                });\n" +
                    "        }\n" +
                    "\n" +
                    "        function sendBroadcast() {\n" +
                    "            const msg = document.getElementById('broadcastMsg').value.trim();\n" +
                    "            if (!msg) return showToast('Vui lòng nhập nội dung cần phát!', false);\n" +
                    "            const formData = new URLSearchParams();\n" +
                    "            formData.append('message', msg);\n" +
                    "            fetch('/api/broadcast', { method: 'POST', body: formData })\n" +
                    "                .then(r => r.text())\n" +
                    "                .then(res => {\n" +
                    "                    showToast(res, res.includes('Đã phát'));\n" +
                    "                    document.getElementById('broadcastMsg').value = '';\n" +
                    "                });\n" +
                    "        }\n" +
                    "\n" +
                    "        function escapeHtml(str) {\n" +
                    "            if (!str) return '';\n" +
                    "            return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\"/g, '&quot;');\n"
                    +
                    "        }\n" +
                    "\n" +
                    "        // ITEM SEARCH & SUBMIT\n" +
                    "        let allItems = [];\n" +
                    "        fetch('/api/items').then(r => r.json()).then(data => { allItems = data; });\n" +
                    "        function filterItems() {\n" +
                    "            const q = document.getElementById('searchBox').value.toLowerCase();\n" +
                    "            const list = document.getElementById('itemList');\n" +
                    "            if(!q) { list.style.display = 'none'; return; }\n" +
                    "            list.style.display = 'block'; list.innerHTML = '';\n" +
                    "            const filtered = allItems.filter(i => i.name.toLowerCase().includes(q)).slice(0, 30);\n"
                    +
                    "            filtered.forEach(i => {\n" +
                    "                const div = document.createElement('div');\n" +
                    "                div.className = 'item-entry';\n" +
                    "                div.innerText = '[' + i.id + '] ' + i.name;\n" +
                    "                div.onclick = () => {\n" +
                    "                    document.getElementById('itemId').value = i.id;\n" +
                    "                    document.getElementById('searchBox').value = i.name;\n" +
                    "                    list.style.display = 'none';\n" +
                    "                };\n" +
                    "                list.appendChild(div);\n" +
                    "            });\n" +
                    "        }\n" +
                    "        function addItem() {\n" +
                    "            const player = document.getElementById('player').value;\n" +
                    "            const itemId = document.getElementById('itemId').value;\n" +
                    "            const quantity = document.getElementById('quantity').value;\n" +
                    "            if(!player || !itemId) return showToast('Vui lòng nhập đủ thông tin!', false);\n" +
                    "            fetch('/api/add-item?player=' + encodeURIComponent(player) + '&itemId=' + itemId + '&quantity=' + quantity)\n"
                    +
                    "            .then(r => r.text())\n" +
                    "            .then(text => showToast(text, text.includes('thành công')))\n" +
                    "            .catch(e => showToast('Lỗi kết nối!', false));\n" +
                    "        }\n" +
                    "        function addMoney() {\n" +
                    "            const player = document.getElementById('moneyPlayer').value;\n" +
                    "            const type = document.getElementById('moneyType').value;\n" +
                    "            const amount = document.getElementById('moneyAmount').value;\n" +
                    "            if(!player || !amount) return showToast('Vui lòng nhập đủ thông tin!', false);\n" +
                    "            fetch('/api/add-money?player=' + encodeURIComponent(player) + '&type=' + type + '&amount=' + amount)\n"
                    +
                    "            .then(r => r.text())\n" +
                    "            .then(text => showToast(text, text.includes('thành công')))\n" +
                    "            .catch(e => showToast('Lỗi kết nối!', false));\n" +
                    "        }\n" +
                    "        function register() {\n" +
                    "            const user = document.getElementById('regUser').value;\n" +
                    "            const pass = document.getElementById('regPass').value;\n" +
                    "            if(!user || !pass) return showToast('Vui lòng nhập đủ thông tin!', false);\n" +
                    "            fetch('/api/register?username=' + encodeURIComponent(user) + '&password=' + encodeURIComponent(pass))\n"
                    +
                    "            .then(r => r.text())\n" +
                    "            .then(text => showToast(text, text.includes('thành công')))\n" +
                    "            .catch(e => showToast('Lỗi kết nối!', false));\n" +
                    "        }\n" +
                    "\n" +
                    "        function loadLoginNotice() {\n" +
                    "            fetch('/api/login-notice')\n" +
                    "                .then(r => r.json())\n" +
                    "                .then(res => {\n" +
                    "                    if (res && res.content !== undefined) {\n" +
                    "                        document.getElementById('loginNoticeContent').value = res.content;\n" +
                    "                    }\n" +
                    "                })\n" +
                    "                .catch(err => showToast('Không thể tải thông báo vào game', false));\n" +
                    "        }\n" +
                    "\n" +
                    "        function saveLoginNotice(sendNow) {\n" +
                    "            const content = document.getElementById('loginNoticeContent').value;\n" +
                    "            const formData = new URLSearchParams();\n" +
                    "            formData.append('content', content);\n" +
                    "            formData.append('sendNow', sendNow ? 'true' : 'false');\n" +
                    "            fetch('/api/login-notice', {\n" +
                    "                method: 'POST',\n" +
                    "                body: formData\n" +
                    "            })\n" +
                    "            .then(r => r.json())\n" +
                    "            .then(res => {\n" +
                    "                showToast(sendNow ? 'Đã lưu & gửi thông báo pop-up tới toàn server!' : 'Đã lưu thông báo đăng nhập thành công!', true);\n"
                    +
                    "            })\n" +
                    "            .catch(e => showToast('Lỗi khi lưu thông báo vào game', false));\n" +
                    "        }\n" +
                    "\n" +
                    "        function clearLoginNotice() {\n" +
                    "            if (!confirm('Bạn có chắc chắn muốn tắt bảng pop-up khi vào game không?')) return;\n" +
                    "            document.getElementById('loginNoticeContent').value = '';\n" +
                    "            saveLoginNotice(false);\n" +
                    "        }\n" +
                    "\n" +
                    "        // Khởi tạo nạp danh sách ban đầu\n" +
                    "        loadNotifies();\n" +
                    "        loadLoginNotice();\n" +
                    "    </script>\n" +
                    "</body>\n" +
                    "</html>";
            sendResponse(exchange, html, "text/html; charset=UTF-8", 200);
        }
    }

    static class NotifiesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            try (Connection conn = LocalManager.getConnection();
                    PreparedStatement ps = conn.prepareStatement("SELECT id, name, text FROM notify ORDER BY id DESC");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!first)
                        sb.append(",");
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String text = rs.getString("text");
                    sb.append("{")
                            .append("\"id\":").append(id).append(",")
                            .append("\"name\":\"").append(escapeJson(name)).append("\",")
                            .append("\"text\":\"").append(escapeJson(text)).append("\"")
                            .append("}");
                    first = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            sb.append("]");
            sendResponse(exchange, sb.toString(), "application/json; charset=UTF-8", 200);
        }
    }

    static class AddNotifyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "";
            try {
                Map<String, String> params = parseParams(exchange);
                String name = params.get("name");
                String text = params.get("text");

                if (name == null || text == null || name.trim().isEmpty() || text.trim().isEmpty()) {
                    response = "Lỗi: Tiêu đề và nội dung không được để trống!";
                } else {
                    boolean success = false;
                    try (Connection conn = LocalManager.getConnection();
                            PreparedStatement ps = conn
                                    .prepareStatement("INSERT INTO notify (name, text) VALUES (?, ?)")) {
                        ps.setString(1, name.trim());
                        ps.setString(2, text.trim());
                        ps.executeUpdate();
                        success = true;
                        response = "Thêm thông báo thành công!";
                    }
                    if (success) {
                        reloadNotify();
                    }
                }
            } catch (Exception e) {
                response = "Lỗi: " + e.getMessage();
            }
            sendResponse(exchange, response, "text/plain; charset=UTF-8", 200);
        }
    }

    static class UpdateNotifyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "";
            try {
                Map<String, String> params = parseParams(exchange);
                int id = Integer.parseInt(params.get("id"));
                String name = params.get("name");
                String text = params.get("text");

                if (name == null || text == null || name.trim().isEmpty() || text.trim().isEmpty()) {
                    response = "Lỗi: Tiêu đề và nội dung không được để trống!";
                } else {
                    boolean success = false;
                    try (Connection conn = LocalManager.getConnection();
                            PreparedStatement ps = conn
                                    .prepareStatement("UPDATE notify SET name = ?, text = ? WHERE id = ?")) {
                        ps.setString(1, name.trim());
                        ps.setString(2, text.trim());
                        ps.setInt(3, id);
                        int updated = ps.executeUpdate();
                        if (updated > 0) {
                            success = true;
                            response = "Cập nhật thông báo #" + id + " thành công!";
                        } else {
                            response = "Lỗi: Không tìm thấy thông báo ID " + id;
                        }
                    }
                    if (success) {
                        reloadNotify();
                    }
                }
            } catch (Exception e) {
                response = "Lỗi: " + e.getMessage();
            }
            sendResponse(exchange, response, "text/plain; charset=UTF-8", 200);
        }
    }

    static class DeleteNotifyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "";
            try {
                Map<String, String> params = parseParams(exchange);
                int id = Integer.parseInt(params.get("id"));

                boolean success = false;
                try (Connection conn = LocalManager.getConnection();
                        PreparedStatement ps = conn.prepareStatement("DELETE FROM notify WHERE id = ?")) {
                    ps.setInt(1, id);
                    int deleted = ps.executeUpdate();
                    if (deleted > 0) {
                        success = true;
                        response = "Xóa thông báo #" + id + " thành công!";
                    } else {
                        response = "Lỗi: Không tìm thấy thông báo ID " + id;
                    }
                }
                if (success) {
                    reloadNotify();
                }
            } catch (Exception e) {
                response = "Lỗi: " + e.getMessage();
            }
            sendResponse(exchange, response, "text/plain; charset=UTF-8", 200);
        }
    }

    static class BroadcastHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "";
            try {
                Map<String, String> params = parseParams(exchange);
                String msg = params.get("message");

                if (msg == null || msg.trim().isEmpty()) {
                    response = "Lỗi: Nội dung thông báo không được để trống!";
                } else {
                    ServerNotify.gI().notify(msg.trim());
                    response = "Đã phát thông báo chạy chữ đến toàn bộ máy chủ!";
                }
            } catch (Exception e) {
                response = "Lỗi: " + e.getMessage();
            }
            sendResponse(exchange, response, "text/plain; charset=UTF-8", 200);
        }
    }

    static class LoginNoticeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String current = getLoginNotice();
                    String json = "{\"status\":\"success\",\"content\":\"" + escapeJson(current) + "\"}";
                    sendResponse(exchange, json, "application/json; charset=UTF-8", 200);
                } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    Map<String, String> params = parseParams(exchange);
                    String content = params.getOrDefault("content", "").trim();
                    String sendNow = params.getOrDefault("sendNow", "false");
                    saveLoginNotice(content);
                    if ("true".equalsIgnoreCase(sendNow) && !content.isEmpty()) {
                        for (Player p : Client.gI().getPlayers()) {
                            if (p != null && p.session != null) {
                                Service.gI().sendThongBaoFromAdmin(p, content);
                            }
                        }
                    }
                    sendResponse(exchange,
                            "{\"status\":\"success\",\"message\":\"Đã lưu thông báo đăng nhập thành công!\"}",
                            "application/json; charset=UTF-8", 200);
                } else {
                    sendResponse(exchange, "Method not allowed", "text/plain", 405);
                }
            } catch (Exception e) {
                sendResponse(exchange, "{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}",
                        "application/json; charset=UTF-8", 500);
            }
        }
    }

    static class AddItemHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "";
            try {
                Map<String, String> query = parseParams(exchange);
                String playerName = query.get("player");
                int itemId = Integer.parseInt(query.get("itemId"));
                int quantity = Integer.parseInt(query.get("quantity"));

                Player player = Client.gI().getPlayer(playerName);
                if (player == null) {
                    response = "Lỗi: Nhân vật '" + playerName + "' không online hoặc không tồn tại!";
                } else {
                    Item item = ItemService.gI().createNewItem((short) itemId, quantity);
                    if (item == null || item.template == null) {
                        response = "Lỗi: ID vật phẩm không hợp lệ!";
                    } else {
                        List<Item.ItemOption> defaultOptions = ItemService.gI().getListOptionItemShop((short) itemId);
                        if (defaultOptions != null && !defaultOptions.isEmpty()) {
                            for (Item.ItemOption option : defaultOptions) {
                                item.itemOptions.add(new Item.ItemOption(option));
                            }
                        } else if (item.isDTS()) {
                            Item dots = ItemService.gI().DoThienSu(itemId, player.gender);
                            if (dots != null && dots.itemOptions != null) {
                                item.itemOptions.addAll(dots.itemOptions);
                            }
                        }
                        InventoryService.gI().addItemBag(player, item);
                        InventoryService.gI().sendItemBags(player);
                        Service.gI().sendThongBao(player,
                                "Bạn nhận được " + quantity + " " + item.template.name + " từ Admin Panel");
                        response = "Thêm thành công " + quantity + " " + item.template.name + " cho " + playerName
                                + "!";
                    }
                }
            } catch (Exception e) {
                response = "Lỗi: " + e.getMessage();
            }
            sendResponse(exchange, response, "text/plain; charset=UTF-8", 200);
        }
    }

    static class AddMoneyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "";
            try {
                Map<String, String> query = parseParams(exchange);
                String playerName = query.get("player");
                int type = Integer.parseInt(query.get("type"));
                long amount = Long.parseLong(query.get("amount"));

                Player player = Client.gI().getPlayer(playerName);
                if (player == null) {
                    response = "Lỗi: Nhân vật '" + playerName + "' không online hoặc không tồn tại!";
                } else {
                    if (type == 1) {
                        player.inventory.gold += amount;
                        if (player.inventory.gold > nro.models.player.Inventory.LIMIT_GOLD)
                            player.inventory.gold = nro.models.player.Inventory.LIMIT_GOLD;
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
            sendResponse(exchange, response, "text/plain; charset=UTF-8", 200);
        }
    }

    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "";
            try {
                Map<String, String> query = parseParams(exchange);
                String user = query.get("username");
                String pass = query.get("password");

                if (user == null || pass == null || user.trim().isEmpty() || pass.trim().isEmpty()) {
                    response = "Lỗi: Tài khoản và mật khẩu không được để trống!";
                } else {
                    try (Connection conn = LocalManager.getConnection()) {
                        PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM account WHERE username = ?");
                        ps.setString(1, user);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            response = "Lỗi: Tài khoản đã tồn tại!";
                        } else {
                            PreparedStatement insert = conn.prepareStatement(
                                    "INSERT INTO account (username, password, email, token, xsrf_token, newpass) VALUES (?, ?, '', '', '', '')");
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
            sendResponse(exchange, response, "text/plain; charset=UTF-8", 200);
        }
    }

    static class ItemsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (nro.models.player_system.Template.ItemTemplate item : Manager.ITEM_TEMPLATES) {
                if (item != null && item.name != null) {
                    if (!first)
                        sb.append(",");
                    sb.append("{\"id\":").append(item.id).append(",\"name\":\"")
                            .append(item.name.replace("\\", "\\\\").replace("\"", "\\\"")).append("\"}");
                    first = false;
                }
            }
            sb.append("]");
            sendResponse(exchange, sb.toString(), "application/json; charset=UTF-8", 200);
        }
    }
}
