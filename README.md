# ZAOP
ZAOP是Android平台上的一个工具类库，结合了AOP思想，基于ASM实现。


# 特点
- 1 使用注解的方式达到效果，使用起来方便直观。
- 2 和业务代码解耦，业务代码逻辑中不需要考虑其他代码。
- 3 基于ASM，代码轻量。

# 使用
### 1.@RTSupport	
配合Android提供的一些编译期注解使用，将其只在编译期作用带入到运行时。<br/>之所以采用@RTSupport配合原生注解的的方式，是想保持编辑器对原生注解的警告支持基础上加入运行时检查。

- 1 @NonNull：作用可以指明一个参数不可以为null，如果传入了null值，开发工具IDE会警告程序可能会有崩溃的风险，但是运行时不会起作用。<br/>配合@RTSupport使用，会在运行时检查标记了@NonNull的参数是否为null，没有标记@NonNull的参数不会加入运行时检查。
```
    //运行时会检查str1 而不会检查str2
    @RTSupport
    public void f(@NonNull String str1, String str2) {
        
    }
```
### 2.过滤快速点击
- 1 默认会对实现OnClickListener.onClick(View)的方法加入快速点击过滤处理，只需要像平常一样使用setOnClickListener()就可以有过滤效果。
```
    //正常使用就已经默认加入了过滤处理
    view.setOnClickListener(new OnClickListener {
        public void onClick(View v){}
    })

    //如果想实现某个OnClick允许快速点击，可以使用@FastClickAllowed, 就不会加入过滤处理了。
    view.setOnClickListener(new OnClickListener {
        @@FastClickAllowed
        public void onClick(View v){}
    })

```


- 2 如果想对OnClickListener.onClick(View)之外的方法(例如使用ButterKnife定义的点击方法)加入快速点击过滤，可以使用@FastClickFilter
```
    @FastClickFilter
    @BindView(R.id.view)
    public void login() {
        
    }
```

### 3.@ThreadOn 
将被标记的方法运行在指定的线程上。
有4种模式：<br/>
ThreadMode.MAIN : 主线程运行<br/>
ThreadMode.POSTING : 保持在被调用的线程运行<br/>
ThreadMode.BACKGROUD : 如果在工作线程被调用就保持在这个的线程，如果在主线程被调用就开一个工作线程运行<br/>
ThreadMode.ASYNC : 无论在哪个线程调用，都新开一个工作线程运行<br/>
```
    @ThreadOn(ThreadMode.MAIN)
    //@ThreadOn(ThreadMode.POSTING)
    //@ThreadOn(ThreadMode.BACKGROUD)
    //@ThreadOn(ThreadMode.ASYNC)
    public void f() {}
```
需要注意的一点：一旦使用到了线程，就代表这个方法就不是同步的变成异步的了，对于有返回值的方法，就没有办法返回正确处理过的返回值了，所以使用了@ThreadOn的方法，建议不要有返回值，如果有返回值，会默认返回这个类型对应的默认值。
```
    //将会返回 0。
    @ThreadOn(ThreadMode.MAIN)
    public int f() {
        return 10
    }
```
