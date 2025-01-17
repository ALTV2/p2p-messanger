package com.tveritin.domain;

import io.ipfs.multihash.Multihash;
import io.libp2p.core.PeerId;
import lombok.Data;

@Data
public class Contact {
    private final String username;
    private final String info;
    private final String originPeerId;
    private final PeerId targetPeerId;
    private final Multihash targetNodeId;
    private final Dialog dialog;
}
