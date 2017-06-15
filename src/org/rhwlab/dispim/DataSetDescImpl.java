package org.rhwlab.dispim;

/**
 *
 * @author gevirl
 */
public class DataSetDescImpl implements DataSetDesc{
    String name;
    String angle;
    String channel;

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getChannel() {
        return this.channel;
    }

    @Override
    public String getAngle()
    {
        return this.angle;
    }
    public void setName(String n){
        this.name = n;
    }
}
