package wasdi;

/**
 * Created by s.adamo on 26/09/2016.
 */
public class Dispatcher implements Runnable{

    SnapOperation m_oSnapOperation = null;

    String m_sFile = null;

    public Dispatcher(SnapOperation oSnapOperation, String sFile)
    {
        this.m_oSnapOperation = oSnapOperation;
        this.m_sFile = sFile;
    }


    public void run() {

        switch (m_oSnapOperation){
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
