package nro.models.boss.Cell;

import nro.models.services.EffectSkillService;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.services.ItemService;
import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstPlayer;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.map.ItemMap;
import nro.models.mob.Mob;
import nro.models.player.Player;
import nro.models.services.PlayerService;
import nro.models.utils.Util;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import nro.models.consts.ConstTaskBadges;
import nro.models.task.BadgesTaskService;

public class SieuBoHung extends Boss {

    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    private long st;
    public boolean callCellCon;
    private long lastTimeChat;
    private long lastTimeMove;
    private int indexChat = 0;
    private final String[] text = {
        "Thưa quý vị và các bạn, đây đúng là trận đấu trời long đất lở",
        "Vượt xa mọi dự đoán của chúng tôi",
        "Eo ơi toàn thân lão Xên bốc cháy kìa"
    };

    public SieuBoHung() throws Exception {
        super(BossID.SIEU_BO_HUNG, BossesData.SIEU_BO_HUNG_1, BossesData.SIEU_BO_HUNG_2);
    }

    @Override
    protected void resetBase() {
        super.resetBase();
        this.callCellCon = false;
    }

    public void callCellCon() {
        executor.submit(() -> {
            try {
                this.changeStatus(BossStatus.AFK);
                this.changeToTypeNonPK();
                this.recoverHP();
                this.callCellCon = true;
                this.chat("Hãy đấu với 7 đứa con của ta, chúng đều là siêu cao thủ");
                Thread.sleep(2000);
                this.chat("Cứ chưởng tiếp đi haha");
                Thread.sleep(2000);
                this.chat("Liệu mà giữ mạng đấy");
                Thread.sleep(2000);
                if (this.bossAppearTogether != null && this.currentLevel < this.bossAppearTogether.length && this.bossAppearTogether[this.currentLevel] != null) {
                    for (Boss boss : this.bossAppearTogether[this.currentLevel]) {
                        if (boss != null) {
                            switch ((int) boss.id) {
                                case BossID.XEN_CON_1, BossID.XEN_CON_2, BossID.XEN_CON_3, BossID.XEN_CON_4, BossID.XEN_CON_5, BossID.XEN_CON_6, BossID.XEN_CON_7 ->
                                    boss.changeStatus(BossStatus.RESPAWN);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    public void recoverHP() {
        PlayerService.gI().hoiPhuc(this, this.nPoint.hpMax, 0);
    }

    @Override
    public void reward(Player plKill) {
        BadgesTaskService.updateCountBagesTask(plKill, ConstTaskBadges.TRUM_SAN_BOSS, 1);
        int diem = 5;
        plKill.event.addEventPoint(diem);
        Service.gI().sendThongBao(plKill, "+5 Point");
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
        ItemService.gI().dropBossReward(this.zone, plKill, this.location.x, this.location.y, 25, 10, 25, 80);
    }

    @Override
    public void active() {
        if (this.typePk == ConstPlayer.NON_PK) {
            this.changeToTypePK();
        }
        this.attack();
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (prepareBom) {
            return 0;
        }

        if (!callCellCon && damage >= this.nPoint.hp) {
            callCellCon();
            return 0;
        }

        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }

            damage = this.nPoint.subDameInjureWithDeff(damage / 3);

            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage /= 4;
            }

            this.nPoint.subHP(damage);

            if (isDie()) {
                setBom(plAtt);
                return 0;
            }

            return (int) damage;
        }
        return 0;
    }

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }

    @Override
    public void autoLeaveMap() {
        this.mc();
        if (this.currentLevel > 0 && this.bossStatus == BossStatus.AFK) {
            this.changeStatus(BossStatus.ACTIVE);
        }
        if (Util.canDoWithTime(st, 900000)) {
            this.leaveMapNew();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }

    public void mc() {
        Player mc = zone.getNpc();
        if (mc != null) {
            if (Util.canDoWithTime(lastTimeChat, 3000)) {
                Service.gI().chat(mc, text[indexChat]);
                indexChat = (indexChat + 1) % text.length;
                lastTimeChat = System.currentTimeMillis() + (indexChat == 0 ? 7000 : 0);
            }

            if (Util.canDoWithTime(lastTimeMove, 15000) && Util.isTrue(2, 3)) {
                int x = this.location.x + Util.nextInt(-100, 100);
                int y = (x > 156 && x < 611) ? 288 : 312;
                PlayerService.gI().playerMove(mc, x, y);
                lastTimeMove = System.currentTimeMillis();
            }
        }
    }
}
