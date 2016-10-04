package it.fadeout;

/**
 * Created by s.adamo on 26/09/2016.
 */
public class Dispatcher implements Runnable{

    Operation m_oOperation = null;

    String m_sFile = null;

    public Dispatcher(Operation oOperation, String sFile)
    {
        this.m_oOperation = oOperation;
        this.m_sFile = sFile;
    }


    public void run() {

        switch (m_oOperation){
            case CALIBRATION:
                break;
            case FILTER:
                break;
            case MULTILOOKING:
                break;
            case TERRAIN:
                break;
        }
    }
}
