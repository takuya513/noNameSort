package noNameSort;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



import tools.MyArrayUtil;
/*
 * volatile を使っている
 */


public class NoNameSort5<E extends Comparable> {
	E[] array;
	int threadsNum,arrayLength,pos = 0,first,mid;
	//	volatile boolean[] isSorted;   //ソート完了部分と未完成部分の判定用
	volatile boolean[] isSorted;   //ソート完了部分と未完成部分の判定用
	ExecutorService executor;
	LinkedList<Callable<Object>> workers;
	int[] posPointer;//次のmiddle or right を覚えておく配列

	static final int UPUP = 2;
	static final int UPDOWN = 3;
	static final int DOWNUP = 4;
	static final int DOWNDOWN = 6;

	public NoNameSort5(){

	}

	public void sort(E[] array){
		this.array = array;
		arrayLength = array.length;
		isSorted = new boolean[arrayLength];
		posPointer = new int[arrayLength];

		Arrays.fill(posPointer, -1);

		//executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		executor = Executors.newFixedThreadPool(2);
		workers = new LinkedList<Callable<Object>>();

		//最初に4パターンに分かれている配列をマージしていき、すべて一パターンの配列にする
		makingWork();

		try {
			printAll();
			executor.invokeAll(workers);
			executor.shutdown();
			printAll();
		} catch (InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

	}





	@SuppressWarnings("unchecked")
	public void makingWork(){
		int pattern,left = 0,num = 0;
		//大きい枠組み
		while(pos <= arrayLength - 1){
			pattern = 1;
			left = pos;
			for(int i = 0;i < 2;i++){		//forで二回回す
				if(array[pos].compareTo(array[pos+1]) <= 0){	//昇順なら
					pattern = pattern * (i + 1);
					//どこまで上に上がるか
					while(array[pos].compareTo(array[pos+1]) <= 0){
						if(pos+1 >= arrayLength-1){
							if(i == 0){
								updateIsSorted(num,left,pos+1);//oxをつける
								posPointer[pos+1] = left;
								posPointer[left] = pos+1;//次のポジションの保存
								System.out.println("LastUp left : "+left+"  pos+1 : "+pos+1);
								workers.add(Executors.callable(new MergeSortWorker(left,pos+1,num++)));
								return;
							}else{
								pos++;
								break;
							}
						}
						pos++;
					}
				}else if(array[pos].compareTo(array[pos+1]) > 0){//降順なら
					pattern = pattern * (i + 2);
					//どこまで下に下がるか
					while(array[pos].compareTo(array[pos+1]) > 0){

						//もしiが0　かつ　最後まで行ってしまったらMergeWorkerに仕事を投げてreturn
						if(pos+1 >= arrayLength-1){
							if(i == 0){
								reverse(left,pos+1);  //すべてが逆順のときだったので逆にして終了
								updateIsSorted(num,left,pos+1);//oxをつける
								posPointer[pos+1] = left;
								posPointer[left] = pos+1;//次のポジションの保存
								System.out.println("LastDWON left : "+left+"  pos+1 : "+pos+1);
								workers.add(Executors.callable(new MergeSortWorker(left,pos+1,num++)));
								return;
							}else{
								pos++;
								break;
							}

						}
						pos++;
					}
				}

				if(i == 0){  //まだ一週目なら様々な処理をする
					mid = pos;
					pos++; //次に進める

					if(pos == arrayLength - 1){
						insertSort(left,pos);
						updateIsSorted(num,left,pos);//oxをつける
						workers.add(Executors.callable(new MergeSortWorker(left,pos,num++)));
						posPointer[pos] = left;
						posPointer[left] = pos;//次のポジションの保存
						return;
					}
				}
			}//for文終了

			//MergeSortWorkerに仕事を投げる
			posPointer[left] = pos;//次のポジションの保存
			posPointer[pos] = left; //前のポジションも保存
			//			Arrays.fill(isSorted,left,mid+1,true);
			//			Arrays.fill(isSorted,mid+1,pos+1,false);
			markBooleanFirst(left,mid,pos);

			System.out.println("left : "+left+"mid : "+mid+"  right : "+pos+"  num : "+num);
			workers.add(Executors.callable(new MergeSortWorker(left,mid,pos,num++,pattern)));
			pos++;  //次に進める
			if(pos == arrayLength-1){
				posPointer[pos] = pos;//次のポジションの保存
				isSorted[pos] = true;   ///修正
				System.out.println("left : "+pos+"  num : "+num);
				workers.add(Executors.callable(new MergeSortWorker(pos,pos,num++)));
				return;
			}
		}//whlie文終了
	}


	public void insertSort(int left,int right){
		int i,j;
		E temp;
		for(i = left + 1; i <=right;i++) {
			temp = array[i];
			j = i;
			while(j > left && temp.compareTo(array[j-1])< 0){
				array[j] = array[j-1];
				j--;
			}
			array[j] = temp;
		}
	}




	class MergeSortWorker implements Runnable{
		int num,nextRight,id;
		int left,right,mid,pattern;
		boolean isRest = false;

		public MergeSortWorker(int left,int mid,int right,int num,int pattern){
			this.num = num;
			this.pattern = pattern;
			this.left = left;
			this.mid = mid;
			this.right = right;
			id = num;

		}


		public MergeSortWorker(int left,int right,int num){
			this.num = num;
			this.left = left;
			this.right = right;
			isRest = true;
			id = num;
		}


		public void run(){
			//printAll();

			printAll("FIRST");
			if(!isRest){   //余りの配列じゃなければ,はじめのソートを行う
				switch(pattern){  //最初のマージだけは様々な形があるのでswitch文で処理
				case UPUP:
					merge(left,mid,right);
					break;
				case UPDOWN:
					mergeUpDown(left,mid,right);
					break;
				case DOWNUP:
					mergeDownUp(left,mid,right);
					break;
				case DOWNDOWN:
					mergeDownDown(left,mid,right);
					break;
				default :
					break;

				}
			}

			updateIsSorted(num,left,right);

			//一回目の処理が終われば次から順次ソートしていく
			do{
				//numが偶数か奇数で分ける
				//偶数であれば
				if(num % 2 == 0){
					printAll();
					//余りであれば　　　このrightがarrayLength - 1で判断する
					if(right == arrayLength - 1){
						//左側を監視し続ける
						while(isSorted[left - 1] != isSorted[left]){
							System.out.println("["+id+"] is wating");
							printAll();
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// TODO 自動生成された catch ブロック
								e.printStackTrace();
							}
						}
						//左がoになったら
						//ソート範囲を拡大しmergeする
						mid = left - 1;
						left = posPointer[mid];

						merge(left,mid,right);

						printAll("rest");
						if(left == 0 && right == arrayLength - 1)
							return; //最後のソートであったら終了


						posPointer[right] = left;
						posPointer[left] = right;	//merge終わったらint[left] = rightに設定
						num = num / 2;	//num = num / 2;

						updateIsSorted(num,left,right);	//booleanを更新
					}else{//余りでなければ
						//右のソートが終わっていれば
						if(isSorted[right] != isSorted[right + 1]){

							//右がxになったら
							//ソート範囲を拡大しmergeする
							mid = right;
							right = posPointer[mid+1];
							merge(left,mid,right);
							printAll("GUUSUU");
							if(left == 0 && right == arrayLength - 1)
								return; //最後のソートであったら終了


							posPointer[right] = left;
							posPointer[left] = right;	//merge終わったらint[left] = rightに設定
							num = num / 2;	//num = num / 2;

							updateIsSorted(num,left,right);	//booleanを更新

						}else {//右のソートが終わっていなければ終了
							return;//終了
						}

					}
				}else{  //奇数であれば
					//左側を見てみる
					if(isSorted[left - 1] != isSorted[left]){  //左のソートが終わっていれば
						//System.out.println("      OOD  : "+id);
						//ソート範囲を拡大しmergeする
						mid = left - 1;
						left = posPointer[mid];

						merge(left,mid,right);
						printAll("ODD");

						if(left == 0 && right == arrayLength - 1)
							return; //最後のソートであったら終了

						posPointer[right] = left;
						posPointer[left] = right;
							num  = (num - 1) / 2;

						updateIsSorted(num,left,right);	//booleanを更新

					}else
						return;
				}
				}while(true);
		}



		private synchronized void printAll() {
			System.out.println(+num+"    left : "+left+"  right : "+right+"num : "+num);
			MyArrayUtil.print(array);
			MyArrayUtil.print(posPointer);
			printBoolean();
			System.out.println();
		}

		private synchronized void printAll(String st) {
			System.out.println(num+ "      "+st);
			System.out.println(+num+"    left : "+left+"  right : "+right+" id : "+id+"num : "+num);
			MyArrayUtil.print(array);
			MyArrayUtil.print(posPointer);
			printBoolean();
			System.out.println();
		}

	}



	private synchronized void updateIsSorted(int num,int left,int right) {
				if(num%2 == 0)	//numが奇数だったらすべてにtrueを代入
					Arrays.fill(isSorted,left,right+1,true);
				else
					Arrays.fill(isSorted,left,right+1,false);

//		if(num%2 == 0){	//numが奇数だったらすべてにtrueを代入
//			for(int i = left;i <= right;i++){
//				isSorted[i] = true;
//			}
//		}else{
//			for(int i = left;i <= right;i++){
//				isSorted[i] = false;
//			}
//		}
	}

	private synchronized void markBooleanFirst(int left,int mid,int right) {
		Arrays.fill(isSorted,left,mid+1,true);
		Arrays.fill(isSorted,mid+1,right+1,false);

	}


	public void merge(int left,int mid,int right){
		Object[] buff = new Object[right - left + 1];
		int i = left,j = mid + 1,k = 0;

		while(i <= mid && j <= right) {
			if(array[i].compareTo(array[j]) < 0)
				buff[k++] = array[i++];
			else
				buff[k++] = array[j++];
		}

		while(i <= mid)
			buff[k++] = array[i++];
		while(j <= right)
			buff[k++] = array[j++];
		for(i = left;i <= right; i++)
			array[i] = (E) buff[i - left];
	}

	private synchronized void printBoolean() {
		for(int i = 0;i < arrayLength;i++){
			if(isSorted[i] == true){
				System.out.print(" o");
			}else{
				System.out.print(" x");
			}
		}
		System.out.println();
	}



	private void reverse(int left, int right) {
		int range = right - left + 1;
		Object[] tmp = new Object[range];

		for(int i = 0;i < range;i++)
			tmp[i] = array[right-i];

		for(int i = 0;i < range;i++){
			array[i + left] = (E) tmp[i];
		}

	}


	public void mergeUpDown(int left,int mid,int right){
		Object[] buff = new Object[right - left + 1];
		int i = left,j = right,k = 0;

		while(i <= mid && j > mid) {
			if(array[i].compareTo(array[j]) < 0)
				buff[k++] = array[i++];
			else
				buff[k++] = array[j--];
		}

		while(i <= mid)
			buff[k++] = array[i++];
		while(j > mid)
			buff[k++] = array[j--];
		for(i = left;i <= right; i++)
			array[i] = (E) buff[i - left];
	}

	public void mergeDownUp(int left,int mid,int right){
		Object[] buff = new Object[right - left + 1];
		int i = mid,j = mid+1,k = 0;

		while(i >= left && j <= right) {
			if(array[i].compareTo(array[j]) < 0)
				buff[k++] = array[i--];
			else
				buff[k++] = array[j++];
		}

		while(i >= left)
			buff[k++] = array[i--];
		while(j <= right)
			buff[k++] = array[j++];
		for(i = left;i <= right; i++)
			array[i] = (E) buff[i - left];
	}

	public void mergeDownDown(int left,int mid,int right){
		Object[] buff = new Object[right - left + 1];
		int i = mid,j = right,k = 0;

		while(i >= left && j > mid) {
			if(array[i].compareTo(array[j]) < 0)
				buff[k++] = array[i--];
			else
				buff[k++] = array[j--];
		}

		while(i >= left)
			buff[k++] = array[i--];
		while(j > mid)
			buff[k++] = array[j--];
		for(i = left;i <= right; i++)
			array[i] = (E) buff[i - left];
	}
	private synchronized void printAll() {
		//System.out.println(+num+"    left : "+left+"  right : "+right+" id : "+id+"num : "+num);
		MyArrayUtil.print(array);
		MyArrayUtil.print(posPointer);
		printBoolean();
		System.out.println();
	}
}
