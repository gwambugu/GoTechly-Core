package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.participants.selector;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.transaction.Context;
import org.jpos.transaction.GroupSelector;

import java.io.Serializable;

public class GroupSelectorParticipant implements GroupSelector, Configurable {
    @Override
    public void setConfiguration(Configuration cfg) throws ConfigurationException {

    }

    @Override
    public String select(long l, Serializable serializable) {
        Context ctx = (Context) serializable;
        String msgType =  ctx.get("CBC_MSG_TYPE");
        return msgType;
    }

    @Override
    public int prepare(long id, Serializable context) {
        return PREPARED;
    }

    @Override
    public void commit(long id, Serializable context) {
        GroupSelector.super.commit(id, context);
    }

    @Override
    public void abort(long id, Serializable context) {
        GroupSelector.super.abort(id, context);
    }


}
