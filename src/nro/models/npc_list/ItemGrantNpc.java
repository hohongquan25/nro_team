package nro.models.npc_list;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import nro.models.consts.ConstMap;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.player_system.Template.ItemTemplate;
import nro.models.server.AdminWebServer;
import nro.models.server.Manager;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;

/** Free item browser placed in the Supermarket map. */
public class ItemGrantNpc extends Npc {

    private static final int MENU_ITEMS = 82300;
    private static final int MENU_QUANTITY = 82301;
    private static final int ITEMS_PER_PAGE = 5;
    private static final String[] CATEGORIES = {
        "Tất cả", "Trang bị", "Tiêu hao", "Tiền tệ", "Vật phẩm khác"
    };
    private static final String[] CATEGORY_MENU = {
        "Tất cả", "Trang bị", "Tiêu hao", "Tiền tệ", "Vật phẩm khác", "Đóng"
    };

    public ItemGrantNpc(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }
        if (this.mapId != ConstMap.SIEU_THI) {
            Service.gI().sendThongBao(player, "Không thể thực hiện tại đây");
            return;
        }
        player.idMark.setNpcItemCategory(0);
        player.idMark.setNpcItemPage(0);
        player.idMark.setNpcItemId(-1);
        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                "Chọn danh mục vật phẩm cần thêm vào hành trang", CATEGORY_MENU);
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }
        if (this.mapId != ConstMap.SIEU_THI) {
            Service.gI().sendThongBao(player, "Chức năng này chỉ có tại Siêu Thị");
            return;
        }

        int menu = player.idMark.getIndexMenu();
        if (menu == ConstNpc.BASE_MENU) {
            if (select >= 0 && select < CATEGORIES.length) {
                player.idMark.setNpcItemCategory(select);
                player.idMark.setNpcItemPage(0);
                showItemPage(player);
            }
        } else if (menu == MENU_ITEMS) {
            handleItemPageSelection(player, select);
        } else if (menu == MENU_QUANTITY) {
            handleQuantitySelection(player, select);
        }
    }

    private void handleItemPageSelection(Player player, int select) {
        List<ItemTemplate> items = getItems(player.idMark.getNpcItemCategory());
        int page = player.idMark.getNpcItemPage();
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, items.size());

        if (select >= 0 && select < end - start) {
            ItemTemplate item = items.get(start + select);
            player.idMark.setNpcItemId(item.id);
            this.createOtherMenu(player, MENU_QUANTITY,
                    "Chọn số lượng " + item.name + " muốn thêm", "x1", "x10", "x99", "Quay lại");
        } else if (select == ITEMS_PER_PAGE) {
            if (page > 0) {
                player.idMark.setNpcItemPage(page - 1);
                showItemPage(player);
            }
        } else if (select == ITEMS_PER_PAGE + 1) {
            if (end < items.size()) {
                player.idMark.setNpcItemPage(page + 1);
                showItemPage(player);
            }
        } else if (select == ITEMS_PER_PAGE + 2) {
            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Chọn danh mục vật phẩm cần thêm vào hành trang", CATEGORY_MENU);
        }
    }

    private void handleQuantitySelection(Player player, int select) {
        if (select == 3) {
            showItemPage(player);
            return;
        }
        if (select < 0 || select > 2) {
            return;
        }

        int[] quantities = {1, 10, 99};
        int itemId = player.idMark.getNpcItemId();
        int quantity = quantities[select];
        Item item = ItemService.gI().createNewItem((short) itemId, quantity);
        if (item == null || item.template == null) {
            Service.gI().sendThongBao(player, "Vật phẩm không tồn tại");
            return;
        }

        List<Item.ItemOption> defaultOptions = ItemService.gI().getListOptionItemShop((short) itemId);
        if (defaultOptions != null && !defaultOptions.isEmpty()) {
            for (Item.ItemOption option : defaultOptions) {
                item.itemOptions.add(new Item.ItemOption(option));
            }
        } else if (item.isDTS()) {
            Item divineItem = ItemService.gI().DoThienSu(itemId, player.gender);
            if (divineItem != null && divineItem.itemOptions != null) {
                for (Item.ItemOption option : divineItem.itemOptions) {
                    item.itemOptions.add(new Item.ItemOption(option));
                }
            }
        } else {
            AdminWebServer.applyDefaultSpecialOptions(item);
        }

        if (InventoryService.gI().addItemBag(player, item)) {
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player,
                    "Đã thêm " + quantity + " " + item.template.name + " vào hành trang");
        }
        showItemPage(player);
    }

    private void showItemPage(Player player) {
        int category = player.idMark.getNpcItemCategory();
        List<ItemTemplate> items = getItems(category);
        int page = player.idMark.getNpcItemPage();
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, items.size());
        StringBuilder message = new StringBuilder(CATEGORIES[category])
                .append(" (trang ").append(page + 1).append(")\n");
        String[] options = new String[ITEMS_PER_PAGE + 4];

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            if (start + i < end) {
                ItemTemplate item = items.get(start + i);
                options[i] = "[" + item.id + "] " + item.name;
                message.append("\n").append(options[i]);
            } else {
                options[i] = "—";
            }
        }
        options[ITEMS_PER_PAGE] = page > 0 ? "Trang trước" : "—";
        options[ITEMS_PER_PAGE + 1] = end < items.size() ? "Trang sau" : "—";
        options[ITEMS_PER_PAGE + 2] = "Đổi danh mục";
        options[ITEMS_PER_PAGE + 3] = "Đóng";
        this.createOtherMenu(player, MENU_ITEMS, message.toString(), options);
    }

    private List<ItemTemplate> getItems(int category) {
        List<ItemTemplate> items = new ArrayList<>();
        for (ItemTemplate item : Manager.ITEM_TEMPLATES) {
            if (item == null || item.name == null || item.name.isBlank()) {
                continue;
            }
            if (matchesCategory(item.type, category)) {
                items.add(item);
            }
        }
        items.sort(Comparator.comparingInt(item -> item.id));
        return items;
    }

    private boolean matchesCategory(byte type, int category) {
        return switch (category) {
            case 0 -> true;
            case 1 -> type >= 0 && type <= 5;
            case 2 -> type >= 6 && type <= 8;
            case 3 -> type == 9 || type == 10 || type == 34;
            case 4 -> type > 10 && type != 34;
            default -> false;
        };
    }
}
