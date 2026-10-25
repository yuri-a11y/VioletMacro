-- Violet Macro bridge for Delta (Roblox)
-- Waits for the app's "3" button (localhost long-poll) and runs macro 3 inside the game:
-- camera up -> kick up -> slot 3 -> jump -> slot 1. No screen touch is injected.
-- Run it alone, or paste it below the part of your script that defines `buffers`.

local CFG = {
    port = 38417,
    cameraUp = true,   -- tilt the camera up for a moment
    kick = true,       -- send the kick remote (needs the `buffers` table from your script)
    slot3 = true,
    afterCamera = 0.05,
    afterKick = 0.15,
    afterSlot3 = 0.01,
    afterJump = 0.0,
    keyHold = 0.03,
    lockout = 0.25,
}

local Players = game:GetService("Players")
local RS = game:GetService("ReplicatedStorage")
local VIM = game:GetService("VirtualInputManager")
local StarterGui = game:GetService("StarterGui")
local LP = Players.LocalPlayer

local genv = (getgenv and getgenv()) or _G
if genv.VioletBridgeStop then pcall(genv.VioletBridgeStop) end
local alive = true
genv.VioletBridgeStop = function() alive = false end

local req = (syn and syn.request) or (http and http.request) or http_request or request
local BASE = "http://127.0.0.1:" .. CFG.port

local function note(text)
    pcall(function()
        StarterGui:SetCore("SendNotification", { Title = "Violet Macro", Text = text, Duration = 2 })
    end)
end

local function pressKey(code)
    pcall(function()
        VIM:SendKeyEvent(true, code, false, game)
        task.wait(CFG.keyHold)
        VIM:SendKeyEvent(false, code, false, game)
    end)
end

local function tiltCamera(hrp)
    local cam = workspace.CurrentCamera
    local oldType, oldSubject, cf = cam.CameraType, cam.CameraSubject, cam.CFrame
    local look = cf.LookVector
    cam.CameraType = Enum.CameraType.Scriptable
    cam.CFrame = CFrame.lookAt(cf.Position, cf.Position + Vector3.new(look.X * 0.3, 1, look.Z * 0.3))
    task.delay(0.12, function()
        local hum = LP.Character and LP.Character:FindFirstChildOfClass("Humanoid")
        cam.CameraSubject = hum or oldSubject
        cam.CameraType = hum and Enum.CameraType.Custom or oldType
    end)
end

local function kickUp(hum, hrp)
    -- a free reference: works as a local when pasted below your script's `buffers`, else falls back to the global
    local buf = (typeof(buffers) == "table" and buffers) or rawget(genv, "buffers")
    if not buf then note("kick skipped: no buffers table"); return false end
    local anim = Instance.new("Animation")
    anim.AnimationId = "rbxassetid://76587445975710"
    pcall(function() hum:LoadAnimation(anim):Play() end)
    local dir = hrp.CFrame.LookVector
    local kickVec = vector.create(dir.X * 0.5, 1, dir.Z * 0.5)
    local remote = RS:WaitForChild("ByteNetReliable")
    remote:FireServer(buffer.fromstring(buf["base"]), { { "kick", 25, false, kickVec } })
    return true
end

local running, lastRun = false, 0

local function macro3()
    if running or os.clock() - lastRun < CFG.lockout then return end
    local char = LP.Character
    local hum = char and char:FindFirstChildOfClass("Humanoid")
    local hrp = char and char:FindFirstChild("HumanoidRootPart")
    if not hum or not hrp then return end
    running = true
    local ok, err = pcall(function()
        if CFG.cameraUp then
            tiltCamera(hrp)
            task.wait(CFG.afterCamera)
        end
        if CFG.kick then
            kickUp(hum, hrp)
            task.wait(CFG.afterKick)
        end
        if CFG.slot3 then
            pressKey(Enum.KeyCode.Three)
            task.wait(CFG.afterSlot3)
        end
        hum:ChangeState(Enum.HumanoidStateType.Jumping)
        hum.Jump = true
        task.wait(CFG.afterJump)
        pressKey(Enum.KeyCode.One)
    end)
    lastRun = os.clock()
    running = false
    if not ok then warn("[violet] macro error: " .. tostring(err)) end
end

if not req then
    note("This executor has no http request function")
    return
end

task.spawn(function()
    local last, shown, fails = -1, false, 0
    while alive do
        local ok, res = pcall(req, { Url = BASE .. "/poll?since=" .. last, Method = "GET" })
        local n = ok and res and res.Body and tonumber(res.Body)
        if n then
            fails = 0
            if not shown then shown = true; note("Connected to the app") end
            if last < 0 then
                last = n -- first answer only syncs the counter
            elseif n > last then
                last = n
                task.spawn(macro3)
            end
        else
            fails += 1
            if fails == 3 then note("App not reachable (is the panel service on?)") end
            task.wait(2)
        end
    end
end)

note("Bridge started")
