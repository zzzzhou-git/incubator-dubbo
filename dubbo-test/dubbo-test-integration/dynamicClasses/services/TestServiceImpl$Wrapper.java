package org.apache.dubbo.common.bytecode;

public class Wrapper1 extends org.apache.dubbo.common.bytecode.Wrapper {

		public Wrapper1() {
	}

	public static String[] pns;
	public static java.util.Map pts;
	public static String[] mns;
	public static String[] dmns;
	public static Class[] mts0;
	
	public String[] getPropertyNames() { 
		return pns; 
	}
	
	public boolean hasProperty(String n){ 
		return pts.containsKey($1); 
	}

	public Class getPropertyType(String n) { 
		return (Class)pts.get($1); 
	}

	public String[] getMethodNames() { 
		return mns; 
	}
	
	public String[] getDeclaredMethodNames() { 
		return dmns; 
	}
	
	public void setPropertyValue(Object o, String n, Object v){ 
		org.apache.dubbo.demo.provider.TestServiceImpl w; 
		
		try { 
			w = ((org.apache.dubbo.demo.provider.TestServiceImpl)$1); 
		} catch(Throwable e) { 
			throw new IllegalArgumentException(e); 
		}
		
		throw new org.apache.dubbo.common.bytecode.NoSuchPropertyException("Not found property \""+$2+"\" filed or setter method in class org.apache.dubbo.demo.provider.TestServiceImpl."); 
	}
	
	public Object getPropertyValue(Object o, String n) { 
		org.apache.dubbo.demo.provider.TestServiceImpl w; 
		
		try { 
			w = ((org.apache.dubbo.demo.provider.TestServiceImpl)$1); 
		} catch(Throwable e) { 
			throw new IllegalArgumentException(e); 
		} 
		
		throw new org.apache.dubbo.common.bytecode.NoSuchPropertyException("Not found property \""+$2+"\" filed or setter method in class org.apache.dubbo.demo.provider.TestServiceImpl."); 
	}
	
	public Object invokeMethod(Object o, String n, Class[] p, Object[] v) throws java.lang.reflect.InvocationTargetException { 
		org.apache.dubbo.demo.provider.TestServiceImpl w; 
		
		try { 
			w = ((org.apache.dubbo.demo.provider.TestServiceImpl)$1); 
		} catch(Throwable e) {
			 throw new IllegalArgumentException(e); 
		} 
		
		try { 
			if( "sayHi".equals( $2 )  &&  $3.length == 1 ) { 
				return ($w)w.sayHi((java.lang.String)$4[0]); 
			} 
		} catch(Throwable e) { 
			throw new java.lang.reflect.InvocationTargetException(e);  
		} 
		
		throw new org.apache.dubbo.common.bytecode.NoSuchMethodException("Not found method \""+$2+"\" in class org.apache.dubbo.demo.provider.TestServiceImpl."); 
	}
}