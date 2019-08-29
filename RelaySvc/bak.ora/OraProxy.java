package relaysvc;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author eugener
 */
public class OraProxy extends TcpProxy {

    public OraProxy(ProxyConfig config) throws Exception {
        super(config);
    }

    @Override
    protected TcpReader getSrcReaderInstance() {
        return new OraReader(true, this, config.getCfg().getIoBufferSize());
    }

    @Override
    protected TcpReader getDstReaderInstance() {
        return new OraReader(false, this, config.getCfg().getIoBufferSize());
    }

}
