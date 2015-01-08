/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/bx/workspace/NDroidAdmin/src/edu/nd/darts/cimon/CimonInterface.aidl
 */
package edu.nd.darts.cimon;
/** 
 * API provided for CIMON service.  Applications which seek to incorporate CIMON services
 * should include and build this aidl file with their project. Applications should bind
 * to {@link edu.nd.darts.cimon.NDroidService}, and use this interface to register and
 * unregister monitoring requests with the service.
 * <p>
 * For periodic monitors and conditional monitors, the client will need to provide a {@link android.os.Messenger}
 * to facilitate the update messages to the client with the periodic values.  The client
 * should implement a Handler to handle the periodic messages.
 * <p>
 * For event notification monitors, the client must provide a {@link android.app.PendingIntent}.
 * The client may define what type of Intent they would prefer to use, to allow notifications
 * during inactive times if needed.  The client should implement and register a Receiver
 * to handle the Intent message defined by the PendingIntent that will be used for notification
 * of the event occurring.
 * <p>
 * For a list of all supported metrics and their associated integer reference, see {@link Metrics}.
 * <p>
 * For assistance in constructing event condition strings in the proper format, clients
 * may include {@link Conditions} in their project, which provides some helper methods
 * for constructing expression strings.
 * <br>
 * 
 * @author darts
 *  
 * @see Metrics
 * @see Conditions
 * @see NDroidService
 * 
 */
public interface CimonInterface extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements edu.nd.darts.cimon.CimonInterface
{
private static final java.lang.String DESCRIPTOR = "edu.nd.darts.cimon.CimonInterface";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an edu.nd.darts.cimon.CimonInterface interface,
 * generating a proxy if needed.
 */
public static edu.nd.darts.cimon.CimonInterface asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof edu.nd.darts.cimon.CimonInterface))) {
return ((edu.nd.darts.cimon.CimonInterface)iin);
}
return new edu.nd.darts.cimon.CimonInterface.Stub.Proxy(obj);
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
case TRANSACTION_getPid:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getPid();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getMeasureCnt:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _result = this.getMeasureCnt(_arg0);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getMetricLong:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
long _arg1;
_arg1 = data.readLong();
long _result = this.getMetricLong(_arg0, _arg1);
reply.writeNoException();
reply.writeLong(_result);
return true;
}
case TRANSACTION_registerPeriodic:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
long _arg1;
_arg1 = data.readLong();
long _arg2;
_arg2 = data.readLong();
boolean _arg3;
_arg3 = (0!=data.readInt());
android.os.Messenger _arg4;
if ((0!=data.readInt())) {
_arg4 = android.os.Messenger.CREATOR.createFromParcel(data);
}
else {
_arg4 = null;
}
int _result = this.registerPeriodic(_arg0, _arg1, _arg2, _arg3, _arg4);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_unregisterPeriodic:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
this.unregisterPeriodic(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_registerEvent:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
long _arg1;
_arg1 = data.readLong();
android.app.PendingIntent _arg2;
if ((0!=data.readInt())) {
_arg2 = android.app.PendingIntent.CREATOR.createFromParcel(data);
}
else {
_arg2 = null;
}
int _result = this.registerEvent(_arg0, _arg1, _arg2);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_unregisterEvent:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.unregisterEvent(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_registerConditional:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
java.lang.String _arg1;
_arg1 = data.readString();
long _arg2;
_arg2 = data.readLong();
android.os.Messenger _arg3;
if ((0!=data.readInt())) {
_arg3 = android.os.Messenger.CREATOR.createFromParcel(data);
}
else {
_arg3 = null;
}
int _result = this.registerConditional(_arg0, _arg1, _arg2, _arg3);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_unregisterConditional:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
this.unregisterConditional(_arg0, _arg1);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements edu.nd.darts.cimon.CimonInterface
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
/**
	 * Get PID of CIMON application.
	 * Used only for testing purposes.
	 */
@Override public int getPid() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getPid, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
	 * Get count of measurements for metric.
	 * Used only for testing pruposes.
	 * 
	 * @param metric     integer representing metric (per {@link Metrics})
	 */
@Override public int getMeasureCnt(int metric) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(metric);
mRemote.transact(Stub.TRANSACTION_getMeasureCnt, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
	 * Obtain current value for metric in synchronous call. For a list of all supported 
	 * metrics and their associated integer reference, see {@link Metrics}.
	 * 
	 * @param metric     integer representing metric (per {@link Metrics})
	 * @param timeout    required freshness of value, in milliseconds.  If currently 
	 *                    cached value is within timeout bounds, cached value will be 
	 *                    returned for most efficient execution. 
	 * @return    current value for metric
	 * 
	 * @see Metrics
	 */
@Override public long getMetricLong(int metric, long timeout) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
long _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(metric);
_data.writeLong(timeout);
mRemote.transact(Stub.TRANSACTION_getMetricLong, _data, _reply, 0);
_reply.readException();
_result = _reply.readLong();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
     * Register a new periodic monitor. Periodic updates of the values will be provided
     * using the Messenger, with the "what" field set to metric, and the object field set
     * to the new value. Clients should implement a Handler to handle the updates from 
     * the Messenger. Data from the monitor will also be stored in a database. These
     * records can be obtained from the CIMON Content Provider using the unique id
     * returned from this call to filter the data table. Callback Messenger may be null 
     * to only store data in database with no callback.
     * <p>
     * For a list of all supported metrics, their associated integer reference, and the
     * object type used for their values, see {@link Metrics}.
     * 
	 * @param metric    integer representing metric (per {@link Metrics}) to monitor
	 * @param period    period between updates (milliseconds), if eavesdrop is true this will
	 *                     represent the maximum allowable period between updates
	 * @param duration    duration to monitor (in milliseconds), 0 for continuous
	 * @param eavesdrop    if true, will provide updates as frequently as they are available
	 *                        due to any active monitors
	 * @param callback    messenger for client callback handler to handle periodic updates
     * @return    unique id of registered monitor, -1 on failure (typically because metric
     *             is not supported on this system)
     * 
     * @see Metrics
     */
@Override public int registerPeriodic(int metric, long period, long duration, boolean eavesdrop, android.os.Messenger callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(metric);
_data.writeLong(period);
_data.writeLong(duration);
_data.writeInt(((eavesdrop)?(1):(0)));
if ((callback!=null)) {
_data.writeInt(1);
callback.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_registerPeriodic, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
     * Unregister periodic monitor.  Metric and unique monitorId will be used to 
     * identify the existing monitor to remove.
     * 
	 * @param metric    integer representing metric (per {@link Metrics}) of registered monitor
	 * @param monitorId    unique id of periodic monitor, returned by register 
	 *                     {@link #registerPeriodic(int, long, long, boolean, Messenger)}
	 *                     
	 * @see #registerPeriodic(int, long, long, boolean, Messenger)
     */
@Override public void unregisterPeriodic(int metric, int monitorId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(metric);
_data.writeInt(monitorId);
mRemote.transact(Stub.TRANSACTION_unregisterPeriodic, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * Register a new event notification monitor. Client will be notified via the Intent
     * defined by PendingIntent callback when the event occurs (when event expression 
     * evaluates to true). 
     * The client may define what type of Intent they would prefer to use, to allow notifications
     * during inactive times if needed.  The client should implement and register a Receiver
     * to handle the Intent message defined by the PendingIntent that will be used for notification
     * of the event occurring.
     * <p>
     * The expressions used for definition of events are boolean expressions which may
     * contain one or multiple conditions.
     * For assistance in constructing event condition strings in the proper format, clients
     * may include {@link Conditions} in their project, which provides some helper methods
     * for constructing expression strings.
     * 
	 * @param expression    event condition string which defines event to monitor
	 * @param period    maximum permitted period between condition state checks, in milliseconds
	 * @param callback    PendingIntent that defines the Intent that client will handle
	 *                     to receive notification of an event trigger
     * @return    unique id of registered monitor, -1 on failure (typically because metric
     *             is not supported on this system)
	 * 
	 * @see Conditions
     */
@Override public int registerEvent(java.lang.String expression, long period, android.app.PendingIntent callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(expression);
_data.writeLong(period);
if ((callback!=null)) {
_data.writeInt(1);
callback.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_registerEvent, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
     * Unregister event notification monitor.  Unique monitorId will be used to 
     * identify the existing monitor to remove.
     * 
	 * @param monitorId    unique id of event notification monitor, from
	 *                     {@link #registerEvent(String, long, PendingIntent)}
	 *                     
	 * @see #registerEvent(String, long, PendingIntent)
     */
@Override public void unregisterEvent(int monitorId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(monitorId);
mRemote.transact(Stub.TRANSACTION_unregisterEvent, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * Register conditional monitor.  This is a hybrid of a periodic monitor and event
     * notification monitor.  This will monitor an event, as defined by an expression string,
     * similar to an event notification monitor. When the expression evaluates to true, a
     * periodic monitor will be activated for the metric and period requested. The periodic
     * monitor will only be active while the expression string evaluates to true.
     * <p>
     * For a list of all supported metrics, their associated integer reference, and the
     * object type used for their values, see {@link Metrics}.
     * <p>
     * The expressions used for definition of events are boolean expressions which may
     * contain one or multiple conditions.
     * For assistance in constructing event condition strings in the proper format, clients
     * may include {@link Conditions} in their project, which provides some helper methods
     * for constructing expression strings.
     * 
	 * @param metric    integer representing metric (per {@link Metrics}) to monitor
	 * @param expression    event condition string which defines condition when periodic
	 *                       monitor should be active
	 * @param period    maximum permitted period between condition state checks, in milliseconds
	 * @param callback    messenger for client callback handler to handle periodic updates
     * @return    unique id of registered monitor, -1 on failure (typically because metric
     *             is not supported on this system)
	 * 
     * @see Metrics
	 * @see Conditions
     */
@Override public int registerConditional(int metric, java.lang.String expression, long period, android.os.Messenger callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(metric);
_data.writeString(expression);
_data.writeLong(period);
if ((callback!=null)) {
_data.writeInt(1);
callback.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_registerConditional, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
     * Unregister conditional monitor. Metric and callback Messenger will be used to 
     * identify the existing monitor to remove.
     * 
	 * @param metric    integer representing metric (per {@link Metrics}) of registered monitor
	 * @param monitorId    unique id of conditional monitor, from
	 *                     {@link #registerConditional(int, String, long, Messenger)}
	 *                     
	 * @see #registerConditional(int, String, long, Messenger)
     */
@Override public void unregisterConditional(int metric, int monitorId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(metric);
_data.writeInt(monitorId);
mRemote.transact(Stub.TRANSACTION_unregisterConditional, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_getPid = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_getMeasureCnt = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_getMetricLong = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_registerPeriodic = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_unregisterPeriodic = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_registerEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_unregisterEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_registerConditional = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_unregisterConditional = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
}
/**
	 * Get PID of CIMON application.
	 * Used only for testing purposes.
	 */
public int getPid() throws android.os.RemoteException;
/**
	 * Get count of measurements for metric.
	 * Used only for testing pruposes.
	 * 
	 * @param metric     integer representing metric (per {@link Metrics})
	 */
public int getMeasureCnt(int metric) throws android.os.RemoteException;
/**
	 * Obtain current value for metric in synchronous call. For a list of all supported 
	 * metrics and their associated integer reference, see {@link Metrics}.
	 * 
	 * @param metric     integer representing metric (per {@link Metrics})
	 * @param timeout    required freshness of value, in milliseconds.  If currently 
	 *                    cached value is within timeout bounds, cached value will be 
	 *                    returned for most efficient execution. 
	 * @return    current value for metric
	 * 
	 * @see Metrics
	 */
public long getMetricLong(int metric, long timeout) throws android.os.RemoteException;
/**
     * Register a new periodic monitor. Periodic updates of the values will be provided
     * using the Messenger, with the "what" field set to metric, and the object field set
     * to the new value. Clients should implement a Handler to handle the updates from 
     * the Messenger. Data from the monitor will also be stored in a database. These
     * records can be obtained from the CIMON Content Provider using the unique id
     * returned from this call to filter the data table. Callback Messenger may be null 
     * to only store data in database with no callback.
     * <p>
     * For a list of all supported metrics, their associated integer reference, and the
     * object type used for their values, see {@link Metrics}.
     * 
	 * @param metric    integer representing metric (per {@link Metrics}) to monitor
	 * @param period    period between updates (milliseconds), if eavesdrop is true this will
	 *                     represent the maximum allowable period between updates
	 * @param duration    duration to monitor (in milliseconds), 0 for continuous
	 * @param eavesdrop    if true, will provide updates as frequently as they are available
	 *                        due to any active monitors
	 * @param callback    messenger for client callback handler to handle periodic updates
     * @return    unique id of registered monitor, -1 on failure (typically because metric
     *             is not supported on this system)
     * 
     * @see Metrics
     */
public int registerPeriodic(int metric, long period, long duration, boolean eavesdrop, android.os.Messenger callback) throws android.os.RemoteException;
/**
     * Unregister periodic monitor.  Metric and unique monitorId will be used to 
     * identify the existing monitor to remove.
     * 
	 * @param metric    integer representing metric (per {@link Metrics}) of registered monitor
	 * @param monitorId    unique id of periodic monitor, returned by register 
	 *                     {@link #registerPeriodic(int, long, long, boolean, Messenger)}
	 *                     
	 * @see #registerPeriodic(int, long, long, boolean, Messenger)
     */
public void unregisterPeriodic(int metric, int monitorId) throws android.os.RemoteException;
/**
     * Register a new event notification monitor. Client will be notified via the Intent
     * defined by PendingIntent callback when the event occurs (when event expression 
     * evaluates to true). 
     * The client may define what type of Intent they would prefer to use, to allow notifications
     * during inactive times if needed.  The client should implement and register a Receiver
     * to handle the Intent message defined by the PendingIntent that will be used for notification
     * of the event occurring.
     * <p>
     * The expressions used for definition of events are boolean expressions which may
     * contain one or multiple conditions.
     * For assistance in constructing event condition strings in the proper format, clients
     * may include {@link Conditions} in their project, which provides some helper methods
     * for constructing expression strings.
     * 
	 * @param expression    event condition string which defines event to monitor
	 * @param period    maximum permitted period between condition state checks, in milliseconds
	 * @param callback    PendingIntent that defines the Intent that client will handle
	 *                     to receive notification of an event trigger
     * @return    unique id of registered monitor, -1 on failure (typically because metric
     *             is not supported on this system)
	 * 
	 * @see Conditions
     */
public int registerEvent(java.lang.String expression, long period, android.app.PendingIntent callback) throws android.os.RemoteException;
/**
     * Unregister event notification monitor.  Unique monitorId will be used to 
     * identify the existing monitor to remove.
     * 
	 * @param monitorId    unique id of event notification monitor, from
	 *                     {@link #registerEvent(String, long, PendingIntent)}
	 *                     
	 * @see #registerEvent(String, long, PendingIntent)
     */
public void unregisterEvent(int monitorId) throws android.os.RemoteException;
/**
     * Register conditional monitor.  This is a hybrid of a periodic monitor and event
     * notification monitor.  This will monitor an event, as defined by an expression string,
     * similar to an event notification monitor. When the expression evaluates to true, a
     * periodic monitor will be activated for the metric and period requested. The periodic
     * monitor will only be active while the expression string evaluates to true.
     * <p>
     * For a list of all supported metrics, their associated integer reference, and the
     * object type used for their values, see {@link Metrics}.
     * <p>
     * The expressions used for definition of events are boolean expressions which may
     * contain one or multiple conditions.
     * For assistance in constructing event condition strings in the proper format, clients
     * may include {@link Conditions} in their project, which provides some helper methods
     * for constructing expression strings.
     * 
	 * @param metric    integer representing metric (per {@link Metrics}) to monitor
	 * @param expression    event condition string which defines condition when periodic
	 *                       monitor should be active
	 * @param period    maximum permitted period between condition state checks, in milliseconds
	 * @param callback    messenger for client callback handler to handle periodic updates
     * @return    unique id of registered monitor, -1 on failure (typically because metric
     *             is not supported on this system)
	 * 
     * @see Metrics
	 * @see Conditions
     */
public int registerConditional(int metric, java.lang.String expression, long period, android.os.Messenger callback) throws android.os.RemoteException;
/**
     * Unregister conditional monitor. Metric and callback Messenger will be used to 
     * identify the existing monitor to remove.
     * 
	 * @param metric    integer representing metric (per {@link Metrics}) of registered monitor
	 * @param monitorId    unique id of conditional monitor, from
	 *                     {@link #registerConditional(int, String, long, Messenger)}
	 *                     
	 * @see #registerConditional(int, String, long, Messenger)
     */
public void unregisterConditional(int metric, int monitorId) throws android.os.RemoteException;
}
