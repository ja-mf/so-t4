/* It is in this file, specifically the load function that will
   be called by MemoryManagement when there is a page fault.  The 
   users of this program should rewrite PageFault to implement the 
   page replacement algorithm as well as the compareTo method from 
   the Page class.
*/

import java.util.*;

public class PageFault {

    /**
     * The page replacement algorithm for the memory management sumulator.
     * This method gets called whenever a page needs to be replaced.
     * <p>
     * <pre>
     *   controlPanel.removeFrame(virtualPageNumber)
     * </pre>
     * Once a page is removed from memory it must also be reflected 
     * graphically. This line does so by removing the physical page 
     * at the virtualPageNumber value. The page which will be added into 
     * memory must also be displayed through the addFrame method.
     * One must also remember to reset the values of 
     * the page which has just been removed from memory.
     */

    public static void load(Page page, Vector frameVector, int currentTime, ControlPanel controlPanel)
    {
        Collections.sort(frameVector);

        MFrame frame = (MFrame) frameVector.firstElement();

        if (frame.used())
        {
            controlPanel.removeFrame(frame.getPageID());
            frame.unload();
        }

        page.load(frame, currentTime);
        controlPanel.addFrame(frame.getFrameID(), page.getPageID());
        controlPanel.pageFaultValueLabel.setText("YES");
    }
}
