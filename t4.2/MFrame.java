public class MFrame implements Comparable
{
    private int frameID;
    private Page page;

    public MFrame(int frameID)
    {
        this.frameID = frameID;
        reset();
    }

    public void load(Page page)
    {
        this.page = page;
    }

    public void unload()
    {
        if (!used()) return;

        page.unload();
        page = null;
    }

    public void set(Page page)
    {
        this.page = page;
    }

    public void reset()
    {
        this.page = null;
    }

    public boolean used()
    {
        return page == null ? false : true;
    }

    /**
     * defines the sort criteria of frames array
     */
    public int compareTo(Object obj)
    {
        MFrame frame = (MFrame) obj;

        {
            // not USED frame first
            if (used() == false && frame.used() == false)
                return frameID < frame.frameID ? -1 : 1;

            if (used() == false) return -1;
            if (frame.used() == false) return 1;
        }

        if (page.getInMemTime() == frame.page.getInMemTime())
            return frameID < frame.frameID ? -1 : 1;
        else
            return page.getInMemTime() < frame.page.getInMemTime() ? -1 : 1;
    }

    public int getFrameID()
    {
        return frameID;
    }

    public int getPageID()
    {
        if (!used()) return -1;

        return page.getPageID();
    }

    public int getR()
    {
        if (!used()) return -1;
        return page.getR();
    }

    public void resetR(int currentTime)
    {
        if (!used()) return;

        page.resetR(currentTime);
    }

	public int getLastTouchTime()		//metodo paa obtener tiempo en que se accesa la pagina a memoria
	{
		if(!used()) return -1;

		return page.getLastTouchTime();
	}
}
