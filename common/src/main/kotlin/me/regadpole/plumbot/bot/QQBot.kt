package me.regadpole.plumbot.bot

import cn.evole.onebot.client.OneBotClient
import cn.evole.onebot.client.core.BotConfig
import cn.evole.onebot.sdk.util.MsgUtils
import io.ktor.http.*
import love.forte.simbot.application.Application
import love.forte.simbot.application.launchApplication
import love.forte.simbot.component.onebot.v11.core.bot.OneBotBotConfiguration
import love.forte.simbot.component.onebot.v11.core.bot.firstOneBotBotManager
import love.forte.simbot.component.onebot.v11.core.useOneBot11
import love.forte.simbot.core.application.Simple
import love.forte.simbot.core.application.launchSimpleApplication
import me.regadpole.plumbot.PlumBot
import me.regadpole.plumbot.listener.qq.QQListener
import taboolib.common.platform.function.info
import taboolib.common.platform.function.submitAsync


class QQBot(private val plugin: PlumBot): Bot {

    private lateinit var onebot: OneBotClient

    /**
     * Start a bot
     */
    override fun start(): Bot {
        onebot = if (plugin.getConfig().getConfig().bot.gocqhttp.hasAccessToken) {
            OneBotClient.create(BotConfig(plugin.getConfig().getConfig().bot.gocqhttp.ws, plugin.getConfig().getConfig().bot.gocqhttp.token)) //创建websocket客户端
                .open() //连接onebot服务端
                .registerEvents(QQListener()) //注册事件监听器
        } else {
            OneBotClient.create(BotConfig(plugin.getConfig().getConfig().bot.gocqhttp.ws)) //创建websocket客户端
                .open() //连接onebot服务端
                .registerEvents(QQListener()) //注册事件监听器
        }
        val groups: List<String> = plugin.getConfig().getConfig().enableGroups
        for (groupID in groups) {
            sendMsg(true, groupID, "PlumBot已启动")
        }
        info("已启动go-cqhttp服务")
        return this
    }

    private suspend fun startOnebot() {
        val app = launchApplication(Simple) {
            useOneBot11()
        }

        app.configure()
        app.join()
    }

    private suspend fun Application.configure() {
        val botManager = botManagers.firstOneBotBotManager()
        val bot = botManager.register(
            OneBotBotConfiguration().apply {
                botUniqueId = "11112222"
                apiServerHost = Url("http://127.0.0.1:3000")
                eventServerHost = Url("ws://127.0.0.1:3001")
                accessToken = null
                /// ...
            }
        )
        bot.start()
    }

    /**
     * Stop a bot
     */
    override fun shutdown() {
        val groups: List<String> = plugin.getConfig().getConfig().enableGroups
        for (groupID in groups) {
            sendMsg(true, groupID, "PlumBot已关闭")
        }
        onebot.close()
        info("已关闭go-cqhttp服务")
    }

    /**
     * Get the group name
     * @param groupId the id of group
     * @return the group name
     */
    override fun getGroupName(groupId: String): String? {
        if (groupId.toLongOrNull() == null) return null
        val group = groupId.toLong()
        return onebot.bot.getGroupInfo(group, false).data.groupName
    }

    /**
     * check if the user is in group
     * @param userId the id of user
     * @param groupId the id of group
     * @return true if the user is in group, false otherwise
     */
    override fun checkUserInGroup(userId: String, groupId: String): Boolean {
        if (userId.toLongOrNull() == null || groupId.toLongOrNull() == null) return false
        val user = userId.toLong(); val group = groupId.toLong()
        for (groupMemberInfoResp in onebot.bot.getGroupMemberList(group).data) {
            if (groupMemberInfoResp.userId == user) return true
        }
        return false
    }

    /**
     * Send a group message
     * @param targetId ID of the target
     * @param message Message to send
     */
    override fun sendGroupMsg(targetId: String, message: String) {
        if (targetId.toLongOrNull() == null) return
        val target = targetId.toLong()
        onebot.bot.sendGroupMsg(target, MsgUtils.builder().text(message).build(), true)
    }

    /**
     * Send a user message
     * @param targetId ID of the target
     * @param message Message to send
     */
    override fun sendUserMsg(targetId: String, message: String) {
        if (targetId.toLongOrNull() == null) return
        val target = targetId.toLong()
        onebot.bot.sendGroupMsg(target, MsgUtils.builder().text(message).build(), true)
    }

    /**
     * Send message to the group/user
     * @param isGroup is this message send to group
     * @param targetId the target you want to send
     * @param message the message
     */
    override fun sendMsg(isGroup: Boolean, targetId: String, message: String?) {
        if (message == null || "" == message) {
            return
        }
        submitAsync(now = true) {
            if (isGroup) {
                sendGroupMsg(targetId, message)
            } else {
                sendUserMsg(targetId, message)
            }
        }
    }

    fun sendCQMsg(isGroup: Boolean, targetId: String, msg: String?) {
        if (msg == null || "" == msg) {
            return
        }
        submitAsync(now = true) {
            if (isGroup) {
                sendGroupCQMsg(targetId, msg)
            } else {
                sendUserCQMsg(targetId, msg)
            }
        }
    }

    /**
     * 发送私聊消息
     */
    private fun sendUserCQMsg(targetId: String, msg: String) {
        if (targetId.toLongOrNull() == null) return
        val target = targetId.toLong()
        onebot.bot.sendPrivateMsg(target, msg, false)
    }

    /**
     * 发送群聊消息
     */
    private fun sendGroupCQMsg(targetId: String, msg: String) {
        if (targetId.toLongOrNull() == null) return
        val target = targetId.toLong()
        onebot.bot.sendGroupMsg(target, msg, false)
    }
}