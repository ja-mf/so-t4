import java.lang.Thread;
import java.io.*;
import java.util.*;

public class Kernel extends Thread
{
    // The number of virtual pages must be fixed at 63 due to
    // dependencies in the GUI
    private static int MaxPages = 63;
    private static int MaxFrames = (MaxPages - 1) / 2;

    private String output = null;
    private static final String lineSeparator = System.getProperty("line.separator");
    private String command_file;
    private String config_file;
    private ControlPanel controlPanel ;
    private Vector pageVector = new Vector();
    private Vector instructVector = new Vector();
    private Vector frameVector = new Vector();

    private String status;
    private boolean doStdoutLog = false;
    private boolean doFileLog = false;
    private int timeStamp;
    private int interval = 10;

	private int lastCheck;	
	private int Tau = 5;

    public int runs;
    public int runcycles;
    public long block = (int) Math.pow(2,12);
    public static byte addressradix = 10;

    public void init(String commands, String config)
    {
        File f = new File(commands);
        command_file = commands;
        config_file = config;
        String line;
        String tmp = null;
        String command = "";
        int i = 0;
        double power = 14;
        long addr = 0;
        long address_limit = (block * (MaxPages+1))-1;

        timeStamp = 0;
		
		lastCheck = 0;


        if (config != null)
        {
            f = new File (config);
            try
            {
                BufferedReader in = new BufferedReader(new FileReader(f));
                while ((line = in.readLine()) != null)
                {
                    if (line.startsWith("numpages"))
                    {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens())
                        {
                            tmp = st.nextToken();
                            MaxPages = Common.s2i(st.nextToken()) - 1;
                            if (MaxPages < 2 || MaxPages > 63)
                            {
                                System.out.println("MemoryManagement: numpages out of bounds.");
                                System.exit(-1);
                            }
                            address_limit = (block * (MaxPages+1))-1;
                            MaxFrames = (MaxPages - 1) / 2;
                        }
                    }

                    if (line.startsWith("pagesize"))
                    {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens())
                        {
                            tmp = st.nextToken();
                            tmp = st.nextToken();
                            if ( tmp.startsWith( "power" ) )
                            {
                                power = (double) Integer.parseInt(st.nextToken());
                                block = (int) Math.pow(2,power);
                            }
                            else
                            {
                                block = Long.parseLong(tmp,10);
                            }
                            address_limit = (block * (MaxPages+1))-1;
                        }

                        if ( block < 64 || block > Math.pow(2,26))
                        {
                            System.out.println("MemoryManagement: pagesize is out of bounds");
                            System.exit(-1);
                        }
                    }
                }
                in.close();
            } catch (IOException e) { /* Handle exceptions */ }

            /**
             * initialize virtual pages
             */
            for (i = 0; i <= MaxPages; i++)
                pageVector.addElement(new Page(i, block));

            /**
             * initialize physical pages (frames)
             */
            for (i = 0; i <= MaxFrames; i++)
                frameVector.addElement(new MFrame(i));

            try
            {
                BufferedReader in = new BufferedReader(new FileReader(f));
                while ((line = in.readLine()) != null)

                {
                    if (line.startsWith("memset"))
                    {
                        Page page;
                        MFrame frame;
                        int pageID;
                        int frameID;
                        int R, M;
                        int inMemTime, lastTouchTime;

                        StringTokenizer st = new StringTokenizer(line);
                        st.nextToken();
                        pageID = Common.s2i(st.nextToken());
                        tmp = st.nextToken();

                        if (tmp.startsWith("x"))
                            frameID = -1;
                        else
                            frameID = Common.s2i(tmp);

                        if (pageID < 0 || pageID > MaxPages)
                        {
                            System.out.println("MemoryManagement: Invalid pageID " + pageID +
                                               " in " + config);
                            System.out.println("MemoryManagement: " + line);
                            System.exit(-1);
                        }

                        if (frameID < -1 || frameID > MaxFrames)
                        {
                            System.out.println("MemoryManagement: Invalid frameID " + frameID +
                                               " in " + config);
                            System.out.println("MemoryManagement: " + line);
                            System.exit(-1);
                        }

                        if (frameID > -1 &&
                                ((MFrame) frameVector.elementAt(frameID)).used())
                        {
                            System.out.println("MemoryManagement: Duplicate physical page's ("
                                               + frameID + ") in " + config);
                            System.exit(-1);
                        }

                        R = Common.s2b(st.nextToken());
                        if (R < 0 || R > 1)
                        {
                            System.out.println("MemoryManagement: Invalid R value in " + config);
                            System.exit(-1);
                        }

                        M = Common.s2b(st.nextToken());
                        if (M < 0 || M > 1)
                        {
                            System.out.println("MemoryManagement: Invalid M value in " + config);
                            System.exit(-1);
                        }

                        inMemTime = Common.s2i(st.nextToken());
                        if (inMemTime < 0)
                        {
                            System.out.println("MemoryManagement: Invalid inMemTime in " + config);
                            System.exit(-1);
                        }

                        lastTouchTime = Common.s2i(st.nextToken());
                        if (lastTouchTime < 0 || lastTouchTime < inMemTime)
                        {
                            System.out.println("MemoryManagement: Invalid lastTouchTime in " + config);
                            System.exit(-1);
                        }

                        page = (Page) pageVector.elementAt(pageID);

                        if (frameID >= 0 && frameID <= MaxFrames)
                        {
                            frame = (MFrame) frameVector.elementAt(frameID);
                            page.setFrame(frame, R, M, inMemTime, lastTouchTime);
                        }
                        else
                        {
                            page.setFrame(null, R, M, inMemTime, lastTouchTime);
                        }
                    }
                    if (line.startsWith("enable_logging"))
                    {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens())
                        {
                            if ( st.nextToken().startsWith( "true" ) )
                            {
                                doStdoutLog = true;
                            }
                        }
                    }
                    if (line.startsWith("log_file"))
                    {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens())
                        {
                            tmp = st.nextToken();
                        }
                        if ( tmp.startsWith( "log_file" ) )
                        {
                            doFileLog = false;
                            output = "tracefile";
                        }
                        else
                        {
                            doFileLog = true;
                            doStdoutLog = false;
                            output = tmp;
                        }
                    }

                    if (line.startsWith("addressradix"))
                    {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens())
                        {
                            tmp = st.nextToken();
                            tmp = st.nextToken();
                            addressradix = Byte.parseByte(tmp);
                            if (addressradix < 0 || addressradix > 20)
                            {
                                System.out.println("MemoryManagement: addressradix out of bounds.");
                                System.exit(-1);
                            }
                        }
                    }
                }
                in.close();
            } catch (IOException e) { /* Handle exceptions */ }
        }

        f = new File ( commands );
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(f));
            while ((line = in.readLine()) != null)
            {
                if (line.startsWith("READ") || line.startsWith("WRITE"))
                {
                    if (line.startsWith("READ"))
                    {
                        command = "READ";
                    }
                    if (line.startsWith("WRITE"))
                    {
                        command = "WRITE";
                    }
                    StringTokenizer st = new StringTokenizer(line);
                    tmp = st.nextToken();
                    tmp = st.nextToken();
                    if (tmp.startsWith("random"))
                    {
                        instructVector.addElement(new Instruction(command,
                                                  Common.randomLong(address_limit )));
                    }
                    else
                    {
                        if (tmp.startsWith( "bin" ))
                        {
                            addr = Long.parseLong(st.nextToken(),2);
                        }
                        else if (tmp.startsWith( "oct" ))
                        {
                            addr = Long.parseLong(st.nextToken(),8);
                        }
                        else if (tmp.startsWith( "hex" ))
                        {
                            addr = Long.parseLong(st.nextToken(),16);
                        }
                        else
                        {
                            addr = Long.parseLong(tmp);
                        }
                        if (0 > addr || addr > address_limit)
                        {
                            System.out.println("MemoryManagement: " + addr +
                                               ", Address out of range (0," +
                                               address_limit +") in " + commands);
                            System.exit(-1);
                        }
                        instructVector.addElement(new Instruction(command,addr));
                    }
                }
            }
            in.close();
        } catch (IOException e) { /* Handle exceptions */ }

        if (instructVector.size() < 1)
        {
            System.out.println("MemoryManagement: no instructions present for execution.");
            System.exit(-1);
        }

        if (doFileLog)
        {
            File trace = new File(output);
            trace.delete();
        }

        runs = 0;
        runcycles = instructVector.size();

        for (i = 0; i <= MaxPages; i++)
        {
            Page page = (Page) pageVector.elementAt(i);
            if (page.loaded() == false)
                controlPanel.removeFrame(i);
            else
                controlPanel.addFrame(page.getFrameID(), i);
        }

        for (i = 0; i < instructVector.size(); i++)
        {
            Instruction instruct = (Instruction) instructVector.elementAt(i);
            if ( instruct.addr < 0 || instruct.addr > address_limit)
            {
                System.out.println("MemoryManagement: Instruction (" + instruct.inst + " " +
                                   instruct.addr + ") out of bounds.");
                System.exit(-1);
            }
        }
    }

    public static int getPageID(long memaddr, int numpages, long block)
    {
        int page = (int) (memaddr / block);

        return (page > numpages ? -1 : page);
    }

    public void setControlPanel(ControlPanel newControlPanel)
    {
        controlPanel = newControlPanel;
    }

    public void getPage(int pageID)
    {
        controlPanel.paintPage((Page) pageVector.elementAt(pageID), timeStamp);
    }

    private void printLogFile(String message)
    {
        String line;
        String temp = "";

        File trace = new File(output);
        if (trace.exists())
        {
            try
            {
                BufferedReader in = new BufferedReader(new FileReader(output));
                while ((line = in.readLine()) != null) {
                    temp = temp + line + lineSeparator;
                }
                in.close();
            }
            catch ( IOException e )
            {
                /* Do nothing */
            }
        }
        try
        {
            PrintStream out = new PrintStream( new FileOutputStream( output ) );
            out.print( temp );
            out.print( message );
            out.close();
        }
        catch (IOException e)
        {
            /* Do nothing */
        }
    }

    public void run()
    {
        step();
        while (runs != runcycles)
        {
            try
            {
                Thread.sleep(2000);
            }
            catch(InterruptedException e)
            {
                /* Do nothing */
            }
            step();
        }
    }

    public void step()
    {
        Instruction instruct = (Instruction) instructVector.elementAt(runs++);

        controlPanel.instructionValueLabel.setText(instruct.inst);
        controlPanel.addressValueLabel.setText(Long.toString(instruct.addr,
                                               addressradix));

        getPage(getPageID(instruct.addr, MaxPages, block));
        if (controlPanel.pageFaultValueLabel.getText() == "YES")
        {
            controlPanel.pageFaultValueLabel.setText("NO");
        }

        if (instruct.inst.startsWith("READ"))
        {
            int pageID = getPageID(instruct.addr, MaxPages, block);
            if (pageID < 0) return;

            Page page = (Page) pageVector.elementAt(pageID);

            if (page.loaded() == false)
            {
                if (doFileLog)
                {
                    printLogFile("READ " +
                                 Long.toString(instruct.addr, addressradix) + " ... page fault" );
                }
                if (doStdoutLog)
                {
                    System.out.println("READ " +
                                       Long.toString(instruct.addr , addressradix) +
                                       " ... page fault" );
                }

                PageFault.load(page, frameVector, timeStamp, controlPanel);
            }

            long result = page.read(instruct.addr, timeStamp);

            if (doFileLog)
            {
                printLogFile("READ " +
                             Long.toString(instruct.addr , addressradix) +
                             (result < 0 ? " ... failed" : " ... okay"));
            }
            if (doStdoutLog)
            {
                System.out.println("READ " +
                                   Long.toString(instruct.addr , addressradix) +
                                   (result < 0 ? " ... failed" : " ... okay"));
            }
        }

        if (instruct.inst.startsWith("WRITE"))
        {
            int pageID = getPageID(instruct.addr, MaxPages, block);
            if (pageID < 0) return;

            Page page = (Page) pageVector.elementAt(pageID);

            if (page.loaded() == false)
            {
                if (doFileLog)
                {
                    printLogFile("WRITE " +
                                 Long.toString(instruct.addr, addressradix) + " ... page fault" );
                }
                if (doStdoutLog)
                {
                    System.out.println("WRITE " +
                                       Long.toString(instruct.addr , addressradix) +
                                       " ... page fault" );
                }

                PageFault.load(page, frameVector, timeStamp, controlPanel);
            }

            long result = page.write(instruct.addr, timeStamp);

            if (doFileLog)
            {
                printLogFile("WRITE " +
                             Long.toString(instruct.addr , addressradix) +
                             (result < 0 ? " ... failed" : " ... okay"));
            }
            if (doStdoutLog)
            {
                System.out.println("WRITE " +
                                   Long.toString(instruct.addr , addressradix) +
                                   (result < 0 ? " ... failed" : " ... okay"));
            }
        }

        controlPanel.timeValueLabel.setText(Integer.toString(timeStamp) +
                                            " (ns)");

		// actualizacion de contadores en tiempo fijo Tau
		if (interval >= Tau || lastCheck + Tau >= timeStamp) {
			for (int i = 0; i < frameVector.size(); i++) {
				MFrame p;
				p = (MFrame) frameVector.elementAt(i);
				p.updatePageRefCounter();
			}
			lastCheck = timeStamp;
		}

        timeStamp += interval;

        //instructVector.remove(instruct);
    }

    public void reset() {
        pageVector.removeAllElements();
        frameVector.removeAllElements();
        instructVector.removeAllElements();
        controlPanel.statusValueLabel.setText("STOP") ;
        controlPanel.timeValueLabel.setText("0") ;
        controlPanel.instructionValueLabel.setText("NONE") ;
        controlPanel.addressValueLabel.setText("NULL") ;
        controlPanel.pageFaultValueLabel.setText("NO") ;
        controlPanel.virtualPageValueLabel.setText("x") ;
        controlPanel.physicalPageValueLabel.setText("0") ;
        controlPanel.RValueLabel.setText("0") ;
        controlPanel.MValueLabel.setText("0") ;
        controlPanel.inMemTimeValueLabel.setText("0") ;
        controlPanel.lastTouchTimeValueLabel.setText("0") ;
        controlPanel.lowValueLabel.setText("0") ;
        controlPanel.highValueLabel.setText("0") ;
        init(command_file, config_file);
    }
}
