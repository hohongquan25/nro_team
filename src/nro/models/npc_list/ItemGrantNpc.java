package nro.models.npc_list;

import nro.models.consts.ConstMap;
import nro.models.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.shop.ShopService;

/** Free item shop NPC placed in the Supermarket map. */
public class ItemGrantNpc extends Npc {

    public ItemGrantNpc(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }
        if (this.mapId != ConstMap.SIEU_THI) {
            Service.gI().sendThongBao(player, "Cửa hàng này chỉ có tại Siêu Thị");
            return;
        }
        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                "Mời bạn chọn vật phẩm miễn phí", "Xem vật phẩm", "Đóng");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }
        if (this.mapId != ConstMap.SIEU_THI) {
            Service.gI().sendThongBao(player, "Cửa hàng này chỉ có tại Siêu Thị");
            return;
        }
        if (player.idMark.isBaseMenu() && select == 0) {
            ShopService.gI().openFreeItemShop(player);
        }
    }
}
