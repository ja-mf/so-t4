public class Page
{
    private int pageID;
    private MFrame frame;

    private int R;			// referenced bit
    private int M;			// modified bit
    private int inMemTime;
    private int lastTouchTime;

    private long startAddr;
    private long endAddr;

    public Page(int pageID, long blockSize)
    {
        this.pageID = pageID;
        this.frame = null;
        this.startAddr = blockSize * pageID;
        this.endAddr = (blockSize * (pageID + 1)) - 1;
    }

    public void setFrame(MFrame frame, int R, int M,
                         int inMemTime, int lastTouchTime)
    {
        if (frame != null)
        {
            this.frame = frame;
            frame.set(this);
        }
        this.R = R;
        this.M = M;
        this.inMemTime = inMemTime;
        this.lastTouchTime = lastTouchTime;
    }

    public int getPageID()
    {
        return pageID;
    }

    public boolean loaded()
    {
        return (frame == null) ? false : true;
    }

    public void load(MFrame frame, int currentTime)
    {
        this.frame = frame;
        this.R = 0;
        this.M = 0;
        this.inMemTime = currentTime;
        this.lastTouchTime = currentTime;
        frame.load(this);
    }

    public void unload()
    {
        this.frame = null;
    }

    public int getFrameID()
    {
        return loaded() ? frame.getFrameID() : -1;
    }

    public long read(long addr, int currentTime)
    {
        if (loaded() == false) return -1;

        R = 1;
        lastTouchTime = currentTime;

        return addr;
    }

    public long write(long addr, int currentTime)
    {
        if (loaded() == false) return -1;

        R = 1;
        M = 1;
        lastTouchTime = currentTime;
        return addr;
    }

    public long getStartAddr()
    {
        return startAddr;
    }

    public long getEndAddr()
    {
        return endAddr;
    }

    public int getR()
    {
        if (!loaded()) return -1;
        return R;
    }

    public void resetR(int currentTime)
    {
        R = 0;
        M = 0;
        inMemTime = currentTime;
        lastTouchTime = currentTime;
    }

    public String getRStat()
    {
        if (loaded() == false) return "";

        return R > 0 ? "1" : "0";
    }

    public int getM()
    {
        if (!loaded()) return -1;
        return M;
    }

    public String getMStat()
    {
        if (loaded() == false) return "";

        return M > 0 ? "1" : "0";
    }

    public int getInMemTime()
    {
        return inMemTime;
    }

    public String getInMemStat(int currentTime)
    {
        if (loaded() == false) return "";

        return Integer.toString(currentTime - inMemTime);
    }

    public int getLastTouchTime()
    {
        return lastTouchTime;
    }

    public String getLastTouchStat(int currentTime)
    {
        if (loaded() == false) return "";

        return Integer.toString(currentTime - lastTouchTime);
    }
}
