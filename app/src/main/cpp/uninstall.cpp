#include <jni.h>
#include <string>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/inotify.h>
#include <sys/stat.h>
#include <android/log.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <sys/resource.h>
#include <sys/system_properties.h>
#include <sys/prctl.h>

#define TAG "venshine"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static const char APP_DIR[] = "/data/data/com.wx.appuninstall";
static const char APP_FILES_DIR[] = "/data/data/com.wx.appuninstall/files";
static const char APP_OBSERVED_FILE[] = "/data/data/com.wx.appuninstall/files/observedFile";
static const char APP_LOCK_FILE[] = "/data/data/com.wx.appuninstall/files/lockFile";
static const char *HOST_ADDR = "www.baidu.com";
static const char *SERVER_ADDR = "http://www.baidu.com";
static const int OK = 0;
static const int ERROR = -1;
static jboolean isCopy = JNI_TRUE;
int watchDescriptor;
int fileDescriptor;
pid_t observer = -1;

#ifdef __cplusplus
extern "C" {
#endif

/**
 * 获取SDK版本号
 */
int get_sdk_version();

/**
 * 上传统计数据
 */
int uploadStatData(jstring versionName, jint versionCode);

/**
 * 监听
 */
int startObserver(void *p_buf);

/**
 * 判断是否进程活着
 */
int isProcessAlive(const char *pid);

/**
 * 记录pid
 */
void writePidFile(const char *pid);

void Java_com_baidu_testuninstall_Monitor_open(JNIEnv *env, jobject thiz, jstring userSerial) {
//    if (userSerial == NULL) {
//        // 执行命令am start -a android.intent.action.VIEW -d $(url)
//        execlp("am", "am", "start", "-a", "android.intent.action.VIEW", "-d", "http://www.baidu.com", (char *) NULL);
//    }
//    else {
//        // 执行命令am start --user userSerial -a android.intent.action.VIEW -d $(url)
//        execlp("am", "am", "start", "--user", env->GetStringUTFChars(userSerial, &isCopy), "-a",
//               "android.intent.action.VIEW", "-d", "http://www.baidu.com", (char *) NULL);
//    }
//    uploadStatData();
//    execlp("am", "am", "start","--user", "0" ,"-a", "android.intent.action.VIEW", "-d", "http://www.baidu.com", (char *)NULL);
}

/**
 * 打开浏览器
 */
void Java_com_wx_appuninstall_Uninstall_browser(JNIEnv *env, jobject thiz, jstring serial) {
    if (serial == NULL) {
        // 执行命令am start -a android.intent.action.VIEW -d $(url)
        execlp("am", "am", "start", "-a", "android.intent.action.VIEW", "-d", SERVER_ADDR, (char *) NULL);
    }
    else {
        // 执行命令am start --user userSerial -a android.intent.action.VIEW -d $(url)
        execlp("am", "am", "start", "--user", env->GetStringUTFChars(serial, &isCopy), "-a",
               "android.intent.action.VIEW", "-d", SERVER_ADDR, (char *) NULL);
    }
}

/**
 * 监控程序
 */
jint Java_com_wx_appuninstall_Uninstall_watch(
        JNIEnv *env, jobject thiz, jobject upload_obj) {

    if (upload_obj == NULL) {
        exit(1);
    }

    // 获得UploadInfo类引用
    jclass upload_cls = env->GetObjectClass(upload_obj);
    if (upload_cls == NULL) {
        exit(1);
    }

    // 判断监听进程是否活着
    if (isProcessAlive(APP_OBSERVED_FILE) == OK) {
        LOGE("watch process already exists");
        return observer;
    }

    // 若被监听文件存在，删除
    FILE *p_observedFile = fopen(APP_OBSERVED_FILE, "r");
    if (p_observedFile != NULL) {
        LOGD("delete observed file");
        remove(APP_OBSERVED_FILE);
        fclose(p_observedFile);
    }

    // 若被监听文件存在，删除
    FILE *p_LockedFile = fopen(APP_LOCK_FILE, "r");
    if (p_LockedFile != NULL) {
        LOGD("delete lock file");
        remove(APP_LOCK_FILE);
        fclose(p_LockedFile);
    }

    // 创建进程
    pid_t pid = fork();
//    prctl(PR_SET_NAME, "m.uninstall");
    // 根据返回值不同做不同操作
    if (pid < 0) {  // 创建进程失败
        LOGE("fork process error!");
    } else if (pid == 0) {  // 创建第一个子进程成功，代码运行在子进程中
        LOGD("fork first process succ pid = %d", getpid());
        setsid();  // 将进程和它当前的对话过程和进程组分离开，并且把它设置成一个新的对话过程的领头进程。
        umask(0);  // 为文件赋予更多的权限，因为继承来的文件可能某些权限被屏蔽
        int pid = fork();
        prctl(PR_SET_NAME, "j.k.l.uninstall");
        if (pid == 0) { // 第二个子进程
            // 保存监听进程id
            LOGD("fork second process succ pid = %d", getpid());
            // 分配缓存，以便读取event，缓存大小等于一个struct inotify_event的大小，这样一次处理一个event
            void *p_buf = malloc(sizeof(struct inotify_event));
            if (p_buf == NULL) {
                LOGD("malloc failed !!!");
                exit(1);
            }
            // 通过linux中的inotify机制来监听应用的卸载。inotify是linux内核用于通知用户空间文件系统变化的机制，文件的添加或卸载等事件都能够及时捕获到。
            if (startObserver(p_buf) != 0) {
                return 0;
            }
            writePidFile(APP_OBSERVED_FILE);

            // 开始监听
            while (1) {
                LOGD("start watch");
                // 调用read函数开始监听，read会阻塞进程
                ssize_t readBytes = read(fileDescriptor, p_buf, sizeof(struct inotify_event));

                // 走到这里说明收到目录被删除的事件
                if (IN_DELETE_SELF == ((struct inotify_event *) p_buf)->mask) {
                    LOGD("IN_DELETE_SELF");
                    // 若文件被删除，可能是已卸载，还需进一步判断app文件夹是否存在
                    FILE *p_appDir = fopen(APP_DIR, "r");
                    if (p_appDir != NULL) {
                        // 应用主目录还在（可能还没有来得及清除），sleep一段时间后再判断
                        sleep(5);
                        p_appDir = fopen(APP_DIR, "r");
                    }
                    // 确认已卸载
                    if (p_appDir == NULL) {
                        LOGD("inotify rm watch");
                        inotify_rm_watch(fileDescriptor, watchDescriptor);
                        break;
                    } else {  // 未卸载，可能用户执行了"清除数据"
                        LOGD("not uninstall");
                        fclose(p_appDir);
                        // 应用没有卸载，重新监听
                        if (startObserver(p_buf) != 0) {
                            return 0;
                        }
                    }

                } else {
                    LOGD("NOT IN_DELETE_SELF");
                }
            }
            LOGD("end watch");
            remove(APP_OBSERVED_FILE);
            remove(APP_LOCK_FILE);
            free(p_buf);

            jfieldID nameFieldID = env->GetFieldID(upload_cls, "versionName", "Ljava/lang/String;"); // 获得属性ID
            jfieldID codeFieldID = env->GetFieldID(upload_cls, "versionCode", "I"); // 获得属性ID
            jfieldID browserFieldID = env->GetFieldID(upload_cls, "isBrowser", "Z");   // 获得属性ID
            jstring versionName = (jstring) env->GetObjectField(upload_obj, nameFieldID);// 获得属性值
            jint versionCode = env->GetIntField(upload_obj, codeFieldID);  // 获得属性值
            jboolean isBrowser = env->GetBooleanField(upload_obj, browserFieldID);    // 获得属性值

            // 上传统计数据
            if (uploadStatData(versionName, versionCode) == 0) {
                LOGD("upload success");
            }

            // 是否打开浏览器
            if (isBrowser) {
                // 执行命令am start --user userSerial -a android.intent.action.VIEW -d $(url)
                execlp("am", "am", "start", "--user", "0", "-a", "android.intent.action.VIEW", "-d",
                       SERVER_ADDR,
                       (char *) NULL);
            }

        } else {
            exit(0);
        }
    } else {
        // 父进程直接退出，使子进程被init进程领养，以避免子进程僵死，同时返回子进程pid
        LOGD("parent process exit");
    }
    return pid;
}

/**
 * 监听
 */
int startObserver(void *p_buf) {

    // 若监听文件所在文件夹不存在，创建文件夹
    FILE *p_filesDir = fopen(APP_FILES_DIR, "r");
    if (p_filesDir == NULL) {
        int filesDirRet = mkdir(APP_FILES_DIR, S_IRWXU | S_IRWXG | S_IXOTH);
        if (filesDirRet == -1) {
            LOGE("create app files dir failed");
            exit(1);
        }
    }

//    if (access(APP_FILES_DIR, F_OK) != 0) {
//        LOGD("folder not exists");
//        if (mkdir(APP_FILES_DIR, 0755) == -1) {
//            LOGE("mkdir failed!");
//            exit(1);
//        }
//    }

    // 若被监听文件不存在，创建监听文件
    FILE *p_observedFile = fopen(APP_OBSERVED_FILE, "r");
    if (p_observedFile == NULL) {
        p_observedFile = fopen(APP_OBSERVED_FILE, "w");
        LOGD("create app observed file");
    }
    fclose(p_observedFile);

    // 创建锁文件，通过检测加锁状态来保证只有一个卸载监听进程
    int lockFileDescriptor = open(APP_LOCK_FILE, O_RDONLY);
    if (lockFileDescriptor == -1) {
        lockFileDescriptor = open(APP_LOCK_FILE, O_CREAT);
        LOGD("create app lock file");
    }
    int lockRet = flock(lockFileDescriptor, LOCK_EX | LOCK_NB);
    if (lockRet == -1) {
        LOGE("watch by other process");
        return ERROR;
    }

    // 初始化inotify进程
    fileDescriptor = inotify_init();
    if (fileDescriptor < 0) {
        LOGE("inotify init failed");
        free(p_buf);
        exit(1);
    }

    // 添加inotify监听器，监听APP_OBSERVED_FILE文件
    watchDescriptor = inotify_add_watch(fileDescriptor, APP_OBSERVED_FILE, IN_ALL_EVENTS);
    if (watchDescriptor < 0) {
        LOGE("inotify watch failed");
        free(p_buf);
        exit(1);
    }
    return OK;
}

/**
 * 上传统计数据
 */
int uploadStatData(jstring versionName, jint versionCode) {
    LOGD("upload stat data");

    struct sockaddr_in serv_addr;
    struct hostent *host;
    int sock = socket(AF_INET, SOCK_STREAM, 0);

    if ((host = gethostbyname(HOST_ADDR)) == NULL) {
        LOGE("host name is null.");
        return ERROR;
    }

    memset(&serv_addr, 0, sizeof(serv_addr));  // 每个字节都用0填充
    serv_addr.sin_family = AF_INET;  // 使用IPv4地址
//    serv_addr.sin_addr.s_addr = inet_addr("192.168.1.1");  // 具体的IP地址
    serv_addr.sin_addr = *((struct in_addr *) host->h_addr);
    serv_addr.sin_port = htons(80);  //端口

    if (connect(sock, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0) {
        LOGE("connect error");
        return ERROR;
    }

    LOGD("connect succ");
    int sdkVersion = get_sdk_version();
    char request[100];
    sprintf(request, "GET /web/index.html?versionName=%s&versionCode=%d&sdkVersion=%d\\r\\n\\r\\n\\", versionName,
            versionCode, sdkVersion);
    if (write(sock, request, strlen(request)) < 0) {
        LOGE("request failed");
        return ERROR;
    }
    LOGD("request success");
    // 关闭套接字
    close(sock);
    return OK;
}

/**
 * 判断是否进程活着
 */
int isProcessAlive(const char *pid) {

    FILE *pidFile;
    char observerPID[32];
    if ((pidFile = fopen(pid, "rb")) == NULL) {
        LOGE("can't open pid file");
        return ERROR;
    }
    // fread(&observerPID,sizeof(observerPID),1,pidFile);
    fscanf(pidFile, "%d", &observer);
    fclose(pidFile);
    if (observer > 1) {
        sprintf(observerPID, "%d/n", observer);
        LOGD("read saved pid");

        if (kill(observer, 0) == 0) {
            LOGD("process is alive");
            return OK;
        }

        LOGD("process is not alive");
    } else {
        LOGD("not read saved pid");
        return ERROR;
    }
}

/**
 * 记录pid
 */
void writePidFile(const char *pid) {
    char str[32];
    int pidFile = open(pid, O_WRONLY | O_TRUNC);
    if (pidFile < 0) {
        LOGE("pid is %d", pidFile);
        exit(1);
    }

    if (flock(pidFile, LOCK_EX | LOCK_NB) < 0) {
        LOGD("cann't lock pid file: %s", pid);
        fprintf(stderr, "can't lock pid file: %s", pid);
        exit(1);
    }

    sprintf(str, "%d/n", getpid());
    ssize_t len = strlen(str);
    ssize_t ret = write(pidFile, str, len);

    if (ret != len) {
        LOGE("can't write pid file: %s", pid);
        fprintf(stderr, "can't write pid file: %s", pid);
        exit(1);
    }
    close(pidFile);
    LOGD("write pid file success");
}

/**
 * 获取SDK版本号
 */
int get_sdk_version() {
    char value[8] = "";
    __system_property_get("ro.build.version.sdk", value);
    return atoi(value);
}

#ifdef __cplusplus
}
#endif
