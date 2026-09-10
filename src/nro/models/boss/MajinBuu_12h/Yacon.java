package nro.models.boss.MajinBuu_12h;


import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.consts.BossStatus;
import nro.models.boss.BossesData;
import static nro.models.consts.BossType.FINAL;
import nro.models.consts.ConstPlayer;
import nro.models.item.Item;
import java.util.List;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.utils.Util;

import nro.models.server.ServerNotify;
import nro.models.services.EffectSkillService;
import nro.models.services.ItemService;
import nro.models.services.SkillService;
import nro.models.services.TaskService;
import nro.models.skill.Skill;
import nro.models.utils.SkillUtil;

public class Yacon extends Boss {

    private long lastTimeTanHinh;

    private long lastTimeAfk;

    private long lastTimeChatAfk;

    private int timeChat;

    public Yacon() throws Exception {
        super(FINAL, BossID.YA_CON, BossesData.YACON);
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
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 100) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                if (Util.canDoWithTime(this.lastTimeTanHinh, 10000) && Util.isTrue(5, 20)) {
                    if (SkillUtil.isUseSkillChuong(this)) {
                        this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 200)),
                                Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 70));
                    } else {
                        this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(10, 40)),
                                Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 50));
                    }
                }
                SkillService.gI().useSkill(this, pl, null, -1, null);
                checkPlayerDie(pl);
                if (!Util.canDoWithTime(this.lastTimeTanHinh, 10000)) {
                    this.nPoint.crit = 100;
                    Service.gI().setPos2(this, pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 200)),
                            10000);
                } else {
                    this.nPoint.crit = 10;
                }
                if (Util.canDoWithTime(this.lastTimeTanHinh, 30000)) {
                    if (Util.isTrue(1, 10)) {
                        String[] chat = {"Mi đâu rồi", "Đồ ăn gian!"};
                        Service.gI().chat(pl, chat[Util.nextInt(chat.length)]);
                        this.lastTimeTanHinh = System.currentTimeMillis();
                    }
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public void afk() {
        if (Util.canDoWithTime(lastTimeChatAfk, timeChat)) {
            this.chat("Đừng vội mừng, ta sẽ hồi sinh và thịt hết bọn mi");
            this.lastTimeChatAfk = System.currentTimeMillis();
            this.timeChat = Util.nextInt(10000, 15000);
        }
        if (Util.canDoWithTime(lastTimeAfk, 60000)) {
            Service.gI().hsChar(this, this.nPoint.hpMax, this.nPoint.mpMax);
            this.changeStatus(BossStatus.CHAT_S);
        }
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

            damage = this.nPoint.subDameInjureWithDeff(damage);

            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
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
    public void die(Player plKill) {
        if (plKill != null) {
            reward(plKill);
            ServerNotify.gI().notify(plKill.name + ": Đã tiêu diệt được " + this.name + " mọi người đều ngưỡng mộ.");
        }
        this.lastTimeAfk = System.currentTimeMillis();
        this.changeStatus(BossStatus.AFK);
    }
}
