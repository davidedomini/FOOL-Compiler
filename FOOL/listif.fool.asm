push 0
lhp
push function0
lhp
sw
lhp
push 1
add
shp
push function1
lhp
sw
lhp
push 1
add
shp
lhp
push function0
lhp
sw
lhp
push 1
add
shp
push function2
lhp
sw
lhp
push 1
add
shp
lhp
push function0
lhp
sw
lhp
push 1
add
shp
push function3
lhp
sw
lhp
push 1
add
shp
push function4
lfp
lfp
stm
ltm
ltm
push -5
add
lw
js
lfp
push 0
lfp
push -6
add
lw
stm
ltm
ltm
lw
push 1
add
lw
js
print
halt

function0:
cfp
lra
push 0
stm
sra
pop
pop
sfp
ltm
lra
js

function1:
cfp
lra
push 0
stm
sra
pop
pop
sfp
ltm
lra
js

function2:
cfp
lra
push 1
stm
sra
pop
pop
sfp
ltm
lra
js

function3:
cfp
lra
push 2
stm
sra
pop
pop
sfp
ltm
lra
js

function4:
cfp
lra
push 1
push 1
beq label0
push 1
lhp
sw
lhp
push 1
add
shp
push 10000
push -4
add
lw
lhp
sw
lhp
lhp
push 1
add
shp
b label1
label0:
push 2
push -1
lhp
sw
lhp
push 1
add
shp
lhp
sw
lhp
push 1
add
shp
push 10000
push -3
add
lw
lhp
sw
lhp
lhp
push 1
add
shp
label1:
stm
sra
pop
sfp
ltm
lra
js