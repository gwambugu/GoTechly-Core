package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.job;



import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.tm.bean.AppQBean;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;


@Service
public class EchoService {

    private static final Logger log = Logger.getLogger(EchoService.class.getName());

    // fixedDelay (not fixedRate): the next echo starts 15s AFTER the previous one finishes,
    // so blocking MUX.request calls to a slow/unreachable corridor can never queue up and
    // hog a scheduler thread. Guards the EOD cron jobs even if the pool were ever reduced.
    @Scheduled(fixedDelay = 15000L)
    public void echo() {
        echoFin();
    }

    public void echoFin() {
        //KE
        if (AppQBean.keMuxMap != null && !AppQBean.keMuxMap.entrySet().isEmpty()) {
            ISOMsg isoMsgKE = createEchoMsg();
            sendEchoMsg(isoMsgKE, "KE", 5000L);
        }

        //UG
        if (AppQBean.ugMuxMap != null && !AppQBean.ugMuxMap.entrySet().isEmpty()) {
            ISOMsg isoMsgUG = createEchoMsg();
            sendEchoMsg(isoMsgUG, "UG", 5000L);
        }

        //TZ
        if (AppQBean.tzMuxMap != null && !AppQBean.tzMuxMap.entrySet().isEmpty()) {
            ISOMsg isoMsgTZ = createEchoMsg();
            sendEchoMsg(isoMsgTZ, "TZ", 5000L);
        }

        //RW
        if (AppQBean.rwMuxMap != null && !AppQBean.rwMuxMap.entrySet().isEmpty()) {
            ISOMsg isoMsgRW = createEchoMsg();
            sendEchoMsg(isoMsgRW, "RW", 5000L);
        }

        //SS
        if (AppQBean.ssMuxMap != null && !AppQBean.ssMuxMap.entrySet().isEmpty()) {
            ISOMsg isoMsgSS = createEchoMsg();
            sendEchoMsg(isoMsgSS, "SS", 5000L);
        }

        //CD
        if (AppQBean.cdMuxMap != null && !AppQBean.cdMuxMap.entrySet().isEmpty()) {
            ISOMsg isoMsgCD = createEchoMsg();
            sendEchoMsg(isoMsgCD, "CD", 5000L);
        }

    }

    public ISOMsg createEchoMsg() {
        ISOMsg m = new ISOMsg();
        Random random = new Random();
        String stan = Integer.valueOf(random.nextInt(1000000)).toString();
        System.out.println("create echo Msg Method");
        Date today = new Date();
        String tranDateTime = String.format("%tm%td%tH%tM%tS", today, today, today, today, today);
        String localTranDateTime = String.format("%ty%tm%td%tH%tM%tS", today, today, today, today, today, today);
        try {
            m.setMTI("1804");
            m.set(7, tranDateTime);
            m.set(11, stan);
            m.set(12, localTranDateTime);
            m.set(24, "831");
            m.set(123, "SMS");
            m.set(126, "CAMPAIGN");
            return m;
        } catch (ISOException ex) {
            System.out.println("echo ISOException: " + ex.getMessage());
        }
        return null;
    }

    public ISOMsg sendEchoMsg(ISOMsg isoRequest, String countryCode, long timeout) {
        ISOMsg resp = null;
        try {
            if (countryCode.toUpperCase().equals("KE")) {
                for (Map.Entry<String, MUX> entry : AppQBean.keMuxMap.entrySet()) {
                    resp = entry.getValue().request(isoRequest, timeout);
                }
            } else if (countryCode.toUpperCase().equals("UG")) {
                for (Map.Entry<String, MUX> entry : AppQBean.ugMuxMap.entrySet()) {
                    resp = entry.getValue().request(isoRequest, timeout);
                }
            } else if (countryCode.toUpperCase().equals("TZ")) {
                for (Map.Entry<String, MUX> entry : AppQBean.tzMuxMap.entrySet()) {
                    resp = entry.getValue().request(isoRequest, timeout);
                }
            } else if (countryCode.toUpperCase().equals("RW")) {
                for (Map.Entry<String, MUX> entry : AppQBean.rwMuxMap.entrySet()) {
                    resp = entry.getValue().request(isoRequest, timeout);
                }
            }
            else if (countryCode.toUpperCase().equals("SS") || countryCode.toUpperCase().equals("SU")) {
                for (Map.Entry<String, MUX> entry : AppQBean.ssMuxMap.entrySet()) {
                    resp = entry.getValue().request(isoRequest, timeout);
                }
            } else if (countryCode.toUpperCase().equals("CD")) {
                for (Map.Entry<String, MUX> entry : AppQBean.cdMuxMap.entrySet()) {
                    resp = entry.getValue().request(isoRequest, timeout);
                }
            }
            return resp;
        } catch (ISOException ex) {
            System.out.println("Send Msg ISOException:" + ex.getMessage());
            return null;
        } catch (Exception ex) {
            System.out.println("Send Msg Exception:" + ex.getMessage());
            return null;
        }
    }
}
