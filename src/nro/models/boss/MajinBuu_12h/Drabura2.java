package nro.models.boss.MajinBuu_12h;


import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.consts.BossStatus;
import nro.models.boss.BossesData;
import nro.models.consts.AppearType;
import static nro.models.consts.BossType.FINAL;
import nro.models.item.Item;
import java.util.List;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.services.EffectSkillService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.utils.Util;
import nro.models.services.TaskService;
import nro.models.map.service.ChangeMapService;
import nro.models.skill.Skill;

public class Drabura2 extends Boss {

    private boolean callBoss = true;

    private long lastTimeJoin;

    public Drabura2() throws Exception {
        super(FINAL, BossID.DRABURA_2, BossesData.DRABURA_2);
    }

    @Override
    public void joinMap() {
        if (zoneFinal != null) {
            this.zone = zoneFinal;
        }
        this.lastTimeJoin = System.currentTimeMillis();
        this.callBoss = false;
        ChangeMapService.gI().changeMap(this, this.zone, Util.nextInt(300, 400), 336);
        this.changeStatus(BossStatus.CHAT_S);
    }

    @Override
    public void reward(Player plKill) {
        int diem = 5;
        plKill.event.addEventPoint(diem);
        Service.gI().sendThongBao(plKill, "+5 Point");
        plKill.fightMabu.changePoint((byte) 10);
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
        ItemService.gI().dropBossReward(this.zone, plKill, this.location.x, this.location.y, 25, 10, 25, 80);
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(200, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }

            if (plAtt != null) {
                switch (plAtt.playerSkill.skillSelect.template.id) {
                    case Skill.KAMEJOKO:
                    case Skill.MASENKO:
                    case Skill.ANTOMIC:
                    case Skill.LIEN_HOAN:
                        return 0;
                }
            }

            if (plAtt.isPl() && Util.isTrue(1, 5)) {
                plAtt.fightMabu.changePercentPoint((byte) 1);
            }
            if (damage >= 20000000) {
                damage = 20000000;
            }

            if (damage >= this.nPoint.hp) {
                this.changeStatus(BossStatus.AFK);
                damage = 0;
            }

            this.nPoint.subHP(damage);

            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }

            return (int) damage;
        } else {
            return 0;
        }
    }

    @Override
    public void rest() {
        int nextLevel = this.currentLevel + 1;
        if (nextLevel >= this.data.length) {
            nextLevel = 0;
        }
        if (this.data[nextLevel].getTypeAppear() == AppearType.DEFAULT_APPEAR
                && Util.canDoWithTime(lastTimeRest, secondsRest * 1000)) {
            this.changeStatus(BossStatus.RESPAWN);
        }

        if (Util.canDoWithTime(lastTimeRest, 5000)) {
            if (!this.callBoss) {
                for (Boss boss : this.bossAppearTogether[this.currentLevel]) {
                    boss.changeStatus(BossStatus.RESPAWN);
                }
                this.callBoss = true;
            }
        }

    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(this.lastTimeJoin, 250000)) {
            this.leaveMap();
        }
    }

    @Override
    public void afk() {
        this.changeToTypeNonPK();
        this.changeStatus(BossStatus.DIE);
    }

    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
    }

}
