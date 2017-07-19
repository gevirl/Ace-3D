package org.rhwlab.dispim;

import mpicbg.spim.data.sequence.ViewSetup;

/**
 *
 * @author gevirl
 */
public class Hdf5DataSetDesc implements DataSetDesc {
    public Hdf5DataSetDesc(ViewSetup setup){
        this.setup = setup;
    }

    @Override
    public String getName() {
        if (setup.hasName()){
            return setup.getName();
        }
        return String.format("%s:%s",this.getChannel(),this.getAngle());
    }

    @Override
    public String getChannel() {
        return setup.getChannel().getName();
    }

    @Override
    public String getAngle() {
        return setup.getAngle().getName();
    }
    
    public int getChannelId(){
        return setup.getChannel().getId();
    }
    public int getAngleId(){
        return setup.getAngle().getId();
    }
    public int getIllumninationId(){
        return setup.getIllumination().getId();
    }
    ViewSetup setup;
}
