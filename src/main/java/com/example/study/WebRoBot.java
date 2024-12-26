package com.example.study;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * 本人微博信息抓取（仅文字部分）
 *
 * @author zhangsan
 * @date 2024/12/26 22:14
 */
@NoArgsConstructor
@Data
public class WebRoBot {
    /**
     * cookie（修改为自己的）
     */
    static final String cookie = "xxxxx";

    /**
     * 用户id（修改为自己的）
     */
    static final String uid = "xxxxx";
    /**
     * 时间格式化
     */
    static DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss Z yyyy", Locale.US);
    static DateTimeFormatter outFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 要存储的文件目录（修改为自己的）
     */
    static String webDirPath = "xxxxx";
    /**
     * 要存储的文件名称（修改为自己的）
     */
    static String webFileName = "web.txt";
    static File webFile = getSaveInfoToFile(webDirPath, webFileName);

    /**
     * 原始文件名称（修改为自己的）
     */
    static String webOriginFileName = "webOrigin.txt";
    static File webOriginInfoFile = getSaveInfoToFile(webDirPath, webOriginFileName);

    @Data
    static class WebUrlResponse {
        private String url;
        private String name;
        private String responseStr;

        public WebUrlResponse(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    public static void main(String[] args) {
        // 该请求，每页显示条数：20（固定的）； page：页码
        WebUrlResponse myWebDetailUrlOnce = new WebUrlResponse("我的微博", "https://weibo.com/ajax/statuses/mymblog?uid=" + uid + "&page=%s&feature=0");
        // 第一页查询（确定条数）
        myWebDetailUrlOnce.setUrl(String.format(myWebDetailUrlOnce.getUrl(), 1));
        // 总条数
        Integer total = JSONObject.parseObject(getUrlInfo(myWebDetailUrlOnce).getResponseStr(), WebInfoDTO.class)
                .getData().getTotal();

        int pageSize = total % 20 > 0 ? (total / 20) + 1 : total / 20;
        pageSize = 2;
        for (int i = 1; i <= pageSize; i++) {
            WebUrlResponse myWebDetailUrl = new WebUrlResponse("我的微博", "https://weibo.com/ajax/statuses/mymblog?uid=" + uid + "&page=%s&feature=0");
            // 第i页
            myWebDetailUrl.setName(myWebDetailUrl.getName() + "，页码" + i);
            myWebDetailUrl.setUrl(String.format(myWebDetailUrl.getUrl(), i));
            // 查询数据
            WebUrlResponse myWebDetail = getUrlInfo(myWebDetailUrl);
            // 写入原始数据到文件
            writeInfoToFile(myWebDetail.getResponseStr() + "\n\n\n", webOriginInfoFile);
            // 解析文件
            WebInfoDTO webInfoDTO = JSONObject.parseObject(myWebDetail.getResponseStr(), WebInfoDTO.class);
            // 获取基础数据
            WebInfoDTO.WebInfo data = webInfoDTO.getData();

            // 每页微博信息详情
            List<WebInfoDTO.ListDTO> list = data.getList();
            // 微博明细
            list.forEach(item -> {
                StringBuilder sbStr = new StringBuilder();
                String createdAt = item.getCreatedAt();
                sbStr.append(ZonedDateTime.parse(createdAt, inputFormatter).format(outFormatter) + "\n");
                // 详情-start
                String text = item.getText();
                if (text.endsWith(">展开</span>")) {
                    Long id = item.getId();
                    WebUrlResponse myWebAllDetailUrl = new WebUrlResponse("我的微博详情", "https://weibo.com/ajax/statuses/longtext?id=" + id);
                    getUrlInfo(myWebAllDetailUrl);
                    // 解析
                    text = myWebAllDetailUrl.getResponseStr();
                    WebInfoAllDTO webInfoAllDTO = JSONObject.parseObject(text, WebInfoAllDTO.class);
                    text = webInfoAllDTO.getData().getLongTextContent();
                }
                sbStr.append(text + "\n\n");
                // 详情-end
                myWebDetailUrl.setResponseStr(sbStr.toString());
                printUrl(myWebDetailUrl);
                // 详情写入文件
                writeInfoToFile(sbStr.toString(), webFile);
            });
        }


    }

    /**
     * 写入信息到文件
     *
     * @param str
     * @param file
     */
    static void writeInfoToFile(String str, File file) {
        try {
            // 追加文件
            FileWriter fileWriter = new FileWriter(file, true);
            // 写入数据
            fileWriter.write(str);
            fileWriter.close();
        } catch (IOException e) {
            System.err.println("写入错误：" + str);
        }
    }

    /**
     * 获取保存信息到文件
     *
     * @param dir
     * @param fileName
     * @return
     */
    private static File getSaveInfoToFile(String dir, String fileName) {
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        String filePath = dirFile.getPath() + File.separator + fileName;
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        file.setWritable(true);

        // 清空文件内容
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write("");
            fileWriter.close();
        } catch (IOException e) {
            System.err.println("清空错误：");
        }
        return file;
    }

    /**
     * 获取基本信息
     */
    static void baseInfo() {
        WebUrlResponse infoUrl = new WebUrlResponse("个人信息", "https://weibo.com/ajax/profile/info?uid=" + uid + "");
        WebUrlResponse infoDetailUrl = new WebUrlResponse("个人备注", "https://weibo.com/ajax/profile/detail?uid=" + uid + "");
        List<WebUrlResponse> webUrlResponseList = List.of(infoUrl, infoDetailUrl);
        webUrlResponseList.forEach(r -> {
            WebUrlResponse urlInfo = getUrlInfo(r);
            printUrl(urlInfo);
        });
    }

    /**
     * 打印信息
     *
     * @param webUrlResponse
     */
    static void printUrl(WebUrlResponse webUrlResponse) {
        System.out.println();
        System.out.println(webUrlResponse.getName());
        System.out.println(webUrlResponse.getResponseStr());
    }

    /**
     * 获取url信息
     *
     * @param webUrlResponse
     */
    static WebUrlResponse getUrlInfo(WebUrlResponse webUrlResponse) {
        try {
            // 创建URL对象，指定要请求的网址
            URL url = new URL(webUrlResponse.getUrl());
            // 打开连接，返回HttpURLConnection对象
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            // 设置请求方法为GET
            connection.setRequestMethod("GET");
            // 设置一些请求头（可选，根据实际需求添加）
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("content-type", "application/json; charset=utf-8");
            connection.setRequestProperty("cookie", cookie);

            // 获取响应码
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 获取输入流来读取响应内容
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                // 输出响应内容
                webUrlResponse.setResponseStr(response.toString());
            } else {
                System.err.println(webUrlResponse.getName() + "  请求失败，响应码: " + responseCode);
            }
            // 关闭连接
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return webUrlResponse;
    }

    /**
     * 微博信息
     */
    @NoArgsConstructor
    @Data
    public static class WebInfoDTO {
        private Integer ok;
        private WebInfo data;

        @NoArgsConstructor
        @Data
        public static class WebInfo {
            private Long sinceId;
            private List<ListDTO> list;
            private Integer statusVisible;
            private Boolean bottomTipsVisible;
            private String bottomTipsText;
            private List<?> topicList;
            private Integer total;
        }

        @NoArgsConstructor
        @Data
        public static class ListDTO {
            private VisibleDTO visible;
            private String createdAt;
            private Long id;
            private String idstr;
            private String mid;
            private String mblogid;
            private UserDTO user;
            private Boolean canEdit;
            private Integer textLength;
            private List<List<?>> annotations;
            private String source;
            private Boolean favorited;
            private String rid;
            private Integer readsCount;
            private List<?> picIds;
            private Integer picNum;
            private Boolean isPaid;
            private Integer mblogVipType;
            private NumberDisplayStrategyDTO numberDisplayStrategy;
            private Integer repostsCount;
            private Integer commentsCount;
            private Integer attitudesCount;
            private Integer attitudesStatus;
            private Boolean isLongText;
            private Integer mlevel;
            private Integer contentAuth;
            private Integer isShowBulletin;
            private CommentManageInfoDTO commentManageInfo;
            private Integer shareRepostType;
            private Integer mblogtype;
            private Boolean showFeedRepost;
            private Boolean showFeedComment;
            private Boolean pictureViewerSign;
            private Boolean showPictureViewer;
            private List<?> rcList;
            private String analysisExtra;
            private String readtimetype;
            private Integer mixedCount;
            private Boolean isShowMixed;
            private Boolean isSinglePayAudio;
            private String text;
            private String textRaw;
            private String regionName;
            private List<?> customIcons;
            private PicInfosDTO picInfos;
            private ContinueTagDTO continueTag;

            @NoArgsConstructor
            @Data
            public static class VisibleDTO {
                private Integer type;
                private Integer listId;
            }

            @NoArgsConstructor
            @Data
            public static class UserDTO {
                private Long id;
                private String idstr;
                private Integer pcNew;
                private String screenName;
                private String profileImageUrl;
                private String profileUrl;
                private Boolean verified;
                private Integer verifiedType;
                private String domain;
                private String weihao;
                private StatusTotalCounterDTO statusTotalCounter;
                private String avatarLarge;
                private String avatarHd;
                private Boolean followMe;
                private Boolean following;
                private Integer mbrank;
                private Integer mbtype;
                private Integer vPlus;
                private Integer userAbility;
                private Boolean planetVideo;
                private List<?> iconList;

                @NoArgsConstructor
                @Data
                public static class StatusTotalCounterDTO {
                    private String totalCntFormat;
                    private String commentCnt;
                    private String repostCnt;
                    private String likeCnt;
                    private String totalCnt;
                }
            }

            @NoArgsConstructor
            @Data
            public static class NumberDisplayStrategyDTO {
                private Integer applyScenarioFlag;
                private Integer displayTextMinNumber;
                private String displayText;
            }

            @NoArgsConstructor
            @Data
            public static class CommentManageInfoDTO {
                private Integer commentManageButton;
                private Integer commentPermissionType;
                private Integer approvalCommentType;
                private Integer commentSortType;
            }

            @NoArgsConstructor
            @Data
            public static class PicInfosDTO {
                // todo 
            }

            @NoArgsConstructor
            @Data
            public static class ContinueTagDTO {
                private String title;
                private String pic;
                private String scheme;
                private Boolean cleaned;
            }
        }
    }

    /**
     * 微博信息详情
     */
    @NoArgsConstructor
    @Data
    public static class WebInfoAllDTO {
        private Integer ok;
        private WebInfo data;

        @NoArgsConstructor
        @Data
        public static class WebInfo {
            private String longTextContent;
            private List<?> urlStruct;
        }
    }
}
