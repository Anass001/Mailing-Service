/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.components;

import java.util.ArrayList;
import java.util.List;

public class RotatorComponent {
    private List<Object> list = new ArrayList<Object>();
    private int listCount = 0;
    private int index = 0;
    private int rotateAfter = 0;
    private int counter = 1;

    public RotatorComponent(List list, int rotation) {
        if (list.isEmpty()) return;
        this.rotateAfter = rotation > 0 ? rotation : 1;
        this.list = list;
        this.listCount = list.size();
        this.index = 0;
        this.counter = 1;
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
        if (this.list.isEmpty()) return null;
        return this.list.get(this.index);
    }

    public synchronized Object getCurrentThenRotate() {
        Object obj = this.getCurrentValue();
        this.rotate();
        return obj;
    }

    public void setCurrentValue(Object value) {
        if (this.list.isEmpty()) return;
        this.list.set(this.index, value);
    }

    public List<Object> getList() {
        return this.list;
    }

    public void setList(List<Object> list) {
        this.list = list;
    }

    public int getListCount() {
        return this.listCount;
    }

    public void setListCount(int listCount) {
        this.listCount = listCount;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getRotateAfter() {
        return this.rotateAfter;
    }

    public void setRotateAfter(int rotateAfter) {
        this.rotateAfter = rotateAfter > 0 ? rotateAfter : 1;
    }

    public int getCounter() {
        return this.counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }
}

