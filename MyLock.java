
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class MyLock implements Lock {
    private final Sync sync;

    abstract private static class Sync extends  AbstractQueuedSynchronizer {
        abstract void lock();

        protected final boolean nofairTryAcquire(int arg){
            //1.第一次加锁
            //2.本线程多次加锁
            Thread currentThread = Thread.currentThread();
            int state = getState();
            if (state == 0){
                if (compareAndSetState(0,arg)){
                    setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                }
            }else if (currentThread == getExclusiveOwnerThread()){
                setState(state+arg);
                return true;
            }
            return false;
        }

        final ConditionObject newCondition() {
            return new ConditionObject();
        }
    }

    static final class NonfairSync extends Sync{
        void lock(){
            if (compareAndSetState(0,1)){
                setExclusiveOwnerThread(Thread.currentThread());
            }else{
                acquire(1);
            }
        }

        @Override
        protected boolean tryAcquire(int arg) {
            return nofairTryAcquire(1);
        }

        @Override
        protected boolean tryRelease(int arg) {
            int c = getState() - arg;
            //如果解锁的不是占有锁的线程，抛异常
            if (Thread.currentThread() != getExclusiveOwnerThread()){
                throw new IllegalMonitorStateException();
            }
            //如果解锁后state为0，要释放锁
            boolean free = false;//标记锁是否释放
            if (c == 0){
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }
    }


    @Override
    public void lock() {
        sync.lock();
    }

    @Override
    public void unlock() {
        sync.release(1);
    }

    public MyLock(){
        sync = new NonfairSync();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    @Override
    public boolean tryLock() {
        return sync.nofairTryAcquire(1);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(time));
    }

    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }
}
