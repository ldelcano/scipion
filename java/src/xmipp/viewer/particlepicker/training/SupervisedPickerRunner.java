package xmipp.viewer.particlepicker.training;


import java.util.logging.Level;
import javax.swing.SwingUtilities;
import org.apache.commons.cli.ParseException;
import xmipp.ij.commons.XmippApplication;
import xmipp.utils.XmippDialog;
import xmipp.utils.XmippWindowUtil;
import xmipp.viewer.particlepicker.ParticlePicker;
import xmipp.viewer.particlepicker.ParticlePickerParams;
import xmipp.viewer.particlepicker.training.gui.SupervisedPickerJFrame;
import xmipp.viewer.particlepicker.training.model.Mode;
import xmipp.viewer.particlepicker.training.model.SupervisedParticlePicker;

public class SupervisedPickerRunner implements Runnable {
    private final ParticlePickerParams params;

   

    public SupervisedPickerRunner(String[] args) throws ParseException {

        params = new ParticlePickerParams(args);

    }


    @Override
    public void run() {
        try
        {
            SupervisedParticlePicker ppicker = null;
            
            if (params.mode == Mode.Manual) 
                ppicker = new SupervisedParticlePicker(params.inputfile, params.outputdir, params.threads, params.fast, params.incore);
            else 
                ppicker = new SupervisedParticlePicker(params.inputfile, params.outputdir, params.mode);
            if(XmippWindowUtil.isScipionCmd())
            {
                ppicker.setPython(params.python);
                ppicker.setScipionScript(params.script);
                ppicker.setProjectId(params.projectid);
                ppicker.setProtId(params.protid);
            }
            new SupervisedPickerJFrame(ppicker);
        } catch (Exception e) {
            System.out.println("Error catched on main");
            ParticlePicker.getLogger().log(Level.SEVERE, e.getMessage(), e);
            XmippDialog.showException(null, e);

        }

    }

    public static void main(String[] args) {
        try {
            SupervisedPickerRunner spr = new SupervisedPickerRunner(args);
            SwingUtilities.invokeLater(spr);
        } catch (Exception e) {
            System.out.println("Error catched on main");
            ParticlePicker.getLogger().log(Level.SEVERE, e.getMessage(), e);
            XmippDialog.showException(null, e);

        }

    }

}
