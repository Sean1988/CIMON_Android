/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/bx/workspace/NDroidAdmin/src/edu/nd/darts/cimon/CimonCallbackInterface.aidl
 */
package edu.nd.darts.cimon;
// Declare any non-default types here with import statements
//import andrid.os.Bundle
/** 
 * Not currently used. Messengers and Intents are used for notifications and
 * updates to avoid blocking. 
 * 
 * @deprecated
 */
public interface CimonCallbackInterface extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements edu.nd.darts.cimon.CimonCallbackInterface
{
private static final java.lang.String DESCRIPTOR = "edu.nd.darts.cimon.CimonCallbackInterface";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an edu.nd.darts.cimon.CimonCallbackInterface interface,
 * generating a proxy if needed.
 */
public static edu.nd.darts.cimon.CimonCallbackInterface asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof edu.nd.darts.cimon.CimonCallbackInterface))) {
return ((edu.nd.darts.cimon.CimonCallbackInterface)iin);
}
return new edu.nd.darts.cimon.CimonCallbackInterface.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_handleLong:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
long _arg1;
_arg1 = data.readLong();
this.handleLong(_arg0, _arg1);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements edu.nd.darts.cimon.CimonCallbackInterface
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public void handleLong(int metric, long value) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(metric);
_data.writeLong(value);
mRemote.transact(Stub.TRANSACTION_handleLong, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_handleLong = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
public void handleLong(int metric, long value) throws android.os.RemoteException;
}
