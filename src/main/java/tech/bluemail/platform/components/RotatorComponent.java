package tech.bluemail.platform.components;

import java.util.*;

public class RotatorComponent
{
    private List<Object> list;
    private int listCount;
    private int index;
    private int rotateAfter;
    private int counter;
    
    public RotatorComponent(final List list, final int rotation) {
        super();
        this.list = new ArrayList<Object>();
        this.listCount = 0;
        this.index = 0;
        this.rotateAfter = 0;
        this.counter = 1;
        if (!list.isEmpty()) {
            this.rotateAfter = ((rotation > 0) ? rotation : 1);
            this.list = (List<Object>)list;
            this.listCount = list.size();
            this.index = 0;
            this.counter = 1;
        }
    }
    
    public void reset() {
        this.index = 0;
        this.counter = 1;
    }
    
    public synchronized void rotate() {
        if (this.counter == this.rotateAfter) {
            ++this.index;
            if (this.index == this.listCount) {
                this.index = 0;
            }
            this.counter = 0;
        }
        ++this.counter;
    }
    
    public synchronized Object getCurrentValue() {
        if (!this.list.isEmpty()) {
            return this.list.get(this.index);
        }
        return null;
    }
    
    public synchronized Object getCurrentThenRotate() {
        final Object obj = this.getCurrentValue();
        this.rotate();
        return obj;
    }
    
    public void setCurrentValue(final Object value) {
        if (!this.list.isEmpty()) {
            this.list.set(this.index, value);
        }
    }
    
    public List<Object> getList() {
        return this.list;
    }
    
    public void setList(final List<Object> list) {
        this.list = list;
    }
    
    public int getListCount() {
        return this.listCount;
    }
    
    public void setListCount(final int listCount) {
        this.listCount = listCount;
    }
    
    public int getIndex() {
        return this.index;
    }
    
    public void setIndex(final int index) {
        this.index = index;
    }
    
    public int getRotateAfter() {
        return this.rotateAfter;
    }
    
    public void setRotateAfter(final int rotateAfter) {
        this.rotateAfter = ((rotateAfter > 0) ? rotateAfter : 1);
    }
    
    public int getCounter() {
        return this.counter;
    }
    
    public void setCounter(final int counter) {
        this.counter = counter;
    }
}
